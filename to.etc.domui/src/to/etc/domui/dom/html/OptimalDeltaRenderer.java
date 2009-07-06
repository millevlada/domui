package to.etc.domui.dom.html;

import java.io.*;
import java.util.*;

import to.etc.domui.dom.*;
import to.etc.domui.server.*;
import to.etc.util.*;

/**
 * <p>This class is used to calculate a delta between the "old" and "new" presentations
 * in a Page. It gets used ONLY when the page has changes in it's tree somewhere.</p>
 *
 * <p>The delta needs to take the rendering process into consideration. Nodes have unique IDs, and
 * this <b>must</b> remain true during <b>every</b> step of the rendering process. If a part of
 * the rendering process causes a duplicate ID (possible only because a node is moved forward/earlier in
 * the tree, when it's new version is rendered before the later "delete" is visited). If this occurs,
 * later rendering commands cannot uniquely identify the node because it's ID refers to two nodes in
 * the browser's DOM.</p>
 * 
 * <p>This code visits all nodes in the page, and creates a to-do list of rendering changes in multiple
 * steps. For nodes whose attributes have changed simply it adds the node to the attribute change list; 
 * this is all that's needed for the node.</p>
 *
 * <p>Nodes that are unchanged are skipped fully.</p>
 * <p>The rest of the nodes are nodes with tree changes; of these we only directly handle containers since
 * they contain the changes to their children. We annotate each node with the deletes and adds of IT's
 * children, so at the end of the 1st loop we have a list of actual changes per node. The goal of this
 * change list is to create all deletes before all inserts, and to minimize the #of deletes and inserts.</p>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 6, 2008
 */
public class OptimalDeltaRenderer {
	static private final boolean DEBUG = false;

	private IBrowserOutput m_o;

	private HtmlRenderer m_html;

	private IRequestContext m_ctx;

	private Page m_page;

	private FullHtmlRenderer m_fullRenderer;

	/**
	 * Info on a changed container. It contains the deletes and adds list, plus
	 * the list of base attribute changes collected thru them. While this is
	 * being built it forms a secondary tree which contains only nodes with
	 * changes. This node exists only for tree nodes that have tree changes.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Jun 7, 2008
	 */
	private static class NodeInfo {
		/** The container this is info about. */
		public NodeContainer node;

		public List<NodeBase> deleteList = Collections.EMPTY_LIST;

		public List<NodeBase> attrChangeList = Collections.EMPTY_LIST;

		public List<NodeBase> addList = Collections.EMPTY_LIST;

		public List<NodeInfo> lowerChanges = Collections.EMPTY_LIST;

		/** When T this node is being ADDED. This means that all changes below it can be handled by issuing a single DELETE for it. */
		public boolean isAdded;

		public boolean isFullRender;

		public NodeInfo(NodeContainer c) {
			node = c;
		}

		public void setFullRerender() {
			isFullRender = true;
			lowerChanges = null;
			attrChangeList = null;
			deleteList = null;
		}

		/**
		 * Adds an attribute change for some child.
		 * @param n
		 */
		public void addAttrChange(NodeBase n) {
			if(isFullRender || isAdded)
				return;
			if(attrChangeList == Collections.EMPTY_LIST)
				attrChangeList = new ArrayList<NodeBase>();
			attrChangeList.add(n);
		}

		public void addDelete(NodeBase b) {
			if(isFullRender || isAdded)
				return;
			if(deleteList == Collections.EMPTY_LIST)
				deleteList = new ArrayList<NodeBase>();
			deleteList.add(b);
		}

		public void addAdd(int ix, NodeBase n) {
			if(isFullRender || isAdded)
				return;
			if(addList == Collections.EMPTY_LIST)
				addList = new ArrayList<NodeBase>();
			addList.add(n);
		}

		public void addChildChange(NodeInfo ch) {
			if(ch == this)
				throw new IllegalStateException("?? Adding myself to my own lower list?! " + this.node);
			if(lowerChanges == Collections.EMPTY_LIST)
				lowerChanges = new ArrayList<NodeInfo>();
			lowerChanges.add(ch);
		}
	}

	/**
	 * Maps all tree-changed NodeContainers to the info about the change.
	 */
	private Map<NodeContainer, NodeInfo> m_infoMap = new HashMap<NodeContainer, NodeInfo>(255);

	public OptimalDeltaRenderer(HtmlRenderer html, IBrowserOutput o) {
		m_o = o;
		m_html = html;
		m_fullRenderer = new FullHtmlRenderer(html, o);
		m_fullRenderer.setXml(true);
		m_html.setRenderMode(HtmlRenderMode.ATTR);
		//		m_html.setUpdating(true);
	}

	public IBrowserOutput o() {
		return m_o;
	}

	public IRequestContext ctx() {
		return m_ctx;
	}

	public Page page() {
		return m_page;
	}

	public void render(IRequestContext ctx, Page page) throws Exception {
		m_ctx = ctx;
		m_page = page;
		page.build();

		if(DEBUG) {
			DumpDirtyStateRenderer.dump(page.getBody());
			System.out.println("--- BEFORE node map: ----");
			if(m_page.getBeforeMap() == null) {
				System.out.println("No before map - no tree changes");
			} else {
				for(String k : m_page.getBeforeMap().keySet()) {
					System.out.println(k + ": " + m_page.getBeforeMap().get(k));
				}
			}
		}

		o().writeRaw("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		o().tag("delta");
		o().endtag();
		calc(page);


		StringBuilder sq = page.getAppendedJS();
		o().tag("eval");
		o().endtag();

		//-- Render all component-requested Javascript code for this phase
		o().text(m_fullRenderer.getCreateJS().toString());
		if(sq != null)
			o().text(sq.toString());

		//-- If a component has requested focus - do it.
		if(page.getFocusComponent() != null) {
			o().text("WebUI.focus('" + page.getFocusComponent().getActualID() + "');");
			page.setFocusComponent(null);
		}

		//-- Handle delayed stuff...
		if(page.getConversation().hasDelayedActions())
			o().writeRaw("WebUI.startPolling();");
		else
			o().writeRaw("WebUI.cancelPolling();");

		o().closetag("eval");
		o().closetag("delta");
	}


	/**
	 * Do a downward traverse of all nodes and annotate changes, collecting attribute changes and
	 * tree changes.
	 * @param page
	 */
	private void calc(Page page) throws Exception {
		//-- Create the BODY node's nodeInfo; this starts the tree of changes.
		NodeInfo root = new NodeInfo(null); // jal 20081111 Body is not the PARENT - it is the 1st node to evaluate.
		//		m_infoMap.put(page.getBody(), root);
		doContainer(root, page.getBody()); // Pass 1: annotation

		if(DEBUG)
			dump(root);

		//-- At this point we have a CHANGE tree which we can render immediately
		renderDeletes(root);
		renderRest(root);
		page.clearDeltaFully();
	}

	private void renderDeletes(NodeInfo ni) throws IOException {
		if(DEBUG)
			System.out.println("  .. renderDeletes for " + ni.node);
		if(ni.isFullRender)
			return;
		if(ni.isAdded) {
			renderDelete(ni.node);
			return;
		}
		for(NodeBase nd : ni.deleteList)
			renderDelete(nd);
		for(NodeInfo t : ni.lowerChanges) {
			if(t == ni)
				throw new IllegalStateException("? Node present on it's own lower list??? " + t);
			renderDeletes(t);
		}
	}

	private void renderDelete(NodeBase nd) throws IOException {
		o().tag("remove");
		o().attr("select", "#" + nd.getActualID());
		o().endAndCloseXmltag();
		o().nl();
	}

	private void renderAttributeChange(NodeBase b) throws Exception {
		if(b instanceof IHtmlDeltaAttributeRenderer) {
			((IHtmlDeltaAttributeRenderer) b).renderAttributeChanges(m_html, this, o());
			return;
		}

		o().tag("changeTagAttributes");
		m_html.setTagless(true);
		m_html.setRenderMode(HtmlRenderMode.ATTR);
		//		m_html.setNewNode(false);
		b.visit(m_html);
		o().endAndCloseXmltag(); // Fully-close tag with />
		o().nl();
	}

	private void renderAdd(NodeContainer parent, NodeBase nd) throws Exception {
		m_html.setRenderMode(HtmlRenderMode.ADDS);
		//		m_html.setNewNode(true);				// Indicate a new node is to be rendered
		if(nd.m_origNewIndex == 0) {
			o().tag("prepend");
			o().attr("select", "#" + parent.getActualID());
			m_html.setTagless(false);
			nd.visit(m_fullRenderer);
			o().closetag("prepend");
		} else {
			o().tag("after");
			o().attr("select", "#" + parent.getChild(nd.m_origNewIndex - 1).getActualID());
			m_html.setTagless(false);
			nd.visit(m_fullRenderer);
			o().closetag("after");
		}
		o().nl();
	}

	private void renderRest(NodeInfo ni) throws Exception {
		if(ni.isFullRender) {
			o().tag("replaceContent");
			o().attr("select", "#" + ni.node.getActualID());
			m_html.setTagless(false);
			m_html.setRenderMode(HtmlRenderMode.REPL);
			//			m_html.setNewNode(true);

			//			ni.node.visit(m_fullRenderer);
			m_fullRenderer.visitChildren(ni.node); // 20080624 jal fix for table in table in table in table..... when paging
			o().closetag("replaceContent");

			renderAttributeChange(ni.node); // 20080820 jal Fix voor ontbrekende attrs als tekstinhoud TextArea wijzigt?
			return;
		}

		for(NodeBase b : ni.attrChangeList)
			renderAttributeChange(b);
		for(NodeBase b : ni.addList)
			renderAdd(ni.node, b);
		for(NodeInfo tni : ni.lowerChanges)
			renderRest(tni);
	}

	/**
	 * Retrieves an existing nodeInfo, or adds a new one for this container.
	 * @param c
	 * @return
	 */
	private NodeInfo makeNodeInfo(NodeContainer c) {
		NodeInfo ni = m_infoMap.get(c);
		if(ni == null) {
			ni = new NodeInfo(c);
			m_infoMap.put(c, ni);
		}
		return ni;
	}

	/**
	 * Handle whatever's needed for updating a base node. If this gets called we're
	 * already certain that the base node exists still; the only thing it can have is
	 * attribute changes.
	 * 
	 * @param n
	 */
	private void doBase(NodeInfo parentInfo, NodeBase n) throws Exception {
		//-- The tree here is not dirty -> I will not be re-rendered. Have my attributes changed?
		if(n.hasChangedAttributes()) {
			//-- Add me to the "change attributes" list of my owner.
			parentInfo.addAttrChange(n); // FIXME must be delta-aware
		}
	}

	/**
	 * Recursive walker.
	 * @param nc
	 */
	private void doContainerChildren(NodeInfo nodeInfo, NodeContainer nc) throws Exception {
		for(int i = 0, len = nc.getChildCount(); i < len; i++) {
			NodeBase n = nc.getChild(i);
			if(n instanceof NodeContainer)
				doContainer(nodeInfo, (NodeContainer) n);
			else
				doBase(nodeInfo, n);
		}
	}

	/**
	 * Annotation pass for a container. Handle changes in this node, then move to the children.
	 * @param n
	 */
	private void doContainer(NodeInfo parentChanges, NodeContainer n) throws Exception {
		//-- Handle tree changes, after build
		n.build();
		NodeBase[] oldl = n.getOldChildren();
		if(oldl != null) {
			/*
			 * There is a tree delta; this is valid ONLY if this node existed earlier. If the node did not exist
			 * earlier we have a logic error: the "upper" node should have seen this node as NEW, so abort.
			 */
			if(!m_page.getBeforeMap().containsKey(n.getActualID())) {
				for(String s : m_page.getBeforeMap().keySet())
					System.out.println("before key=" + s);
				throw new IllegalStateException("Rotary device exception: delta exists on NEW node, and we're trying to render the new node as a delta!? Node=" + n.getActualID());
			}

			//-- We have a tree delta -> handle it,
			doTreeDeltaOn(parentChanges, n);
			return;
		}

		//-- The tree here is not dirty -> I will not be re-rendered. Have my attributes changed?
		if(n.hasChangedAttributes()) {
			//-- Add me to the "change attributes" list of my owner.
			parentChanges.addAttrChange(n); // FIXME must be delta-aware
		}

		//-- If one of my children is dirty I need to walk my children, else I'm done.
		if(n.childHasUpdates()) {
			doContainerChildren(parentChanges, n);
		}
		n.clearDelta();
	}

	/**
	 * Called with a node which HAS a changed tree. This creates the annotation (todo list) 
	 * for this node as a NodeInfo structure. It may happen that the node has no changes in
	 * it' list after all after parsing; in this case no deltanode is created.
	 *
	 * @param nc
	 */
	private void doTreeDeltaOn(NodeInfo parentInfo, NodeContainer nc) throws Exception {
		if(DEBUG)
			System.out.println("... deltaing tree on " + nc.getActualID());
		NodeBase[] oldar = nc.getOldChildren();
		List<NodeBase> newl = nc.internalGetChildren();
		NodeInfo ni = makeNodeInfo(nc);

		/*
		 * Optimize: if the #of nodes in old is way smaller than the #nodes in new
		 * then it is better to fully re-render anew. This also handles the case where
		 * old.size == 0, where a re-render is always needed [if new nodes exist].
		 */
		if(oldar.length == 0 || (oldar.length < newl.size() && ((double) oldar.length / newl.size() < 0.10)) || nc.mustRenderChildrenFully()) {
			//-- Full re-render of this node.
			if(DEBUG)
				System.out.println("o: add full children re-render for container=" + nc.getActualID());
			ni.setFullRerender(); // Indicate a full re-render of this node's children and be gone
			if(parentInfo != null)
				parentInfo.addChildChange(ni);
			return;
		}

		/*
		 * Handle deletes: we can skip the nodes in the new list for this because they exist here so
		 * they are either NEW or they moved from another node; this will then be dirty AND have that
		 * node in it's OLD list (it was deleted there).
		 */
		List<NodeBase> oldl = new ArrayList<NodeBase>(oldar.length); // Max size of old, after all deletes, is never > old
		for(int i = 0, len = oldar.length; i < len; i++) { // Walk all old nodes
			NodeBase n = oldar[i];

			/*
			 * A node is DELETED from here if it's CURRENT parent is not the current 
			 * container (including null). If the current parent is the current container
			 * this node is not deleted at all and can at most have moved within the
			 * container.
			 */
			if(n.getParent() != nc) {
				//-- Deleted thingy. Add to the current node's DELETE charge.
				ni.addDelete(n); // This node is DELETED from here,
				if(DEBUG)
					System.out.println("o: add 'delete' node " + n.getActualID() + " from old-parent " + nc.getActualID());
				//				n.clearDelta();					// 2000115 jal Must clear oldParent or Hell Freezeth Over exception...
			} else
				oldl.add(n); // Not deleted; this is now at the location where it would be if all deletes were executed.
		}

		/*
		 * FIXME
		 * Must optimize here!!! compare:
		 * - the number of deletes [d]
		 * - the number of nodes left in old, meaning nodes that are CERTAIN to remain.
		 * If the number of deletes is way bigger than the # remaining it may be
		 * better to do a full re-render.
		 * The number of nodes in the 'new' tree does not matter here, because all nodes
		 * there that are not in the old tree must be rendered anew anyhow, so the only
		 * thing we can win on is to spare us deletes.
		 * This handles the case where a complete subtree is replaced with another subtree: this
		 * is always done best by a re-render (in this case the #of deletes is old.size, the 
		 * remaining old is 0).
		 */

		/*
		 * Primary deletes are known: all nodes that moved to another tree OR that were removed
		 * are gone. What's left are moves and adds. Handle all ADDS now. 
		 */
		List<NodeBase> nl = new ArrayList<NodeBase>(newl.size());
		for(int i = 0; i < newl.size(); i++) {
			NodeBase nn = newl.get(i);
			nn.m_origNewIndex = i; // The actual index for the new node.

			//-- Is this an addition from somewhere else? If so handle it here && remove from the working list
			if(nn.getOldParent() == null || nn.getOldParent() != nc || !m_page.getBeforeMap().containsKey(nn.getActualID())) {
				//-- Came from somewhere else or is new -> render.
				/*
				 * This node is NEW in this tree. We're pretty sure we need to ADD it then. This has
				 * consequences for all deletes below the add-point: we can discard all changes below
				 * this new node provided we render a DELETE for the node instead; this single delete
				 * will delete all of it's children too.
				 */
				if(nn instanceof NodeContainer) {
					if(DEBUG)
						System.out.println("o: new container=" + nn.getActualID() + " added @" + i + " marked as ADDED");
					// FIXME Mark the <<nn>> node as ADDED, causing a full delete and a discard of its childrens commands
					NodeInfo nni = makeNodeInfo((NodeContainer) nn);// Get this node's changeset
					nni.isAdded = true; // Mark as added
				} else {
					if(DEBUG)
						System.out.println("o: new leaf node=" + nn.getActualID() + " added @" + i);
				}

				//-- Append an ADD command regardless of node type. The collector takes care of merging adds into an 'add multiple nodes' if needed
				ni.addAdd(i, nn); // Add @ loc
			} else {
				nl.add(nn);
			}
		}
		if(DEBUG)
			System.out.println("o: before move check, listsize=" + oldl.size());

		/*
		 * What's left are only moves and nodes that have simply changed (not tree-changed). We use the
		 * "change distance" between moves to decide on delete or insert.
		 */
		if(oldl.size() != nl.size()) {
			//-- Dump state
			System.out.println("----- Hell freezer's state ------\nOLD size=" + oldl.size() + ", new size=" + nl.size() + "\nOLD dump:");
			for(NodeBase b : oldl)
				System.out.println("Node=" + b);
			System.out.println("NEW dump:");
			for(NodeBase b : nl)
				System.out.println("Node=" + b);
			System.out.println("----- Hell freezer's state DONE ------");


			throw new IllegalStateException("The impossible has happened (Hell freezeth over?) or there's a huge algorithmic error... My bet's on the second one, so....");
		}
		for(int i = nl.size(); --i >= 0;)
			nl.get(i).m_newNodeIndex = i;
		for(int i = oldl.size(); --i >= 0;)
			oldl.get(i).m_oldNodeIndex = i;

		int olen = oldl.size();
		int nlen = nl.size();
		int oix = 0;
		int nix = 0;
		while(oix < olen) {
			//-- We walk all OLD nodes 1st; when old nodes are gone we are sure we have only adds @ end.
			NodeBase on = oldl.get(oix); // Get "old" node.
			NodeBase nn = nix < nlen ? nl.get(nix) : null; // If another NEW is available...
			if(on == null)
				throw new IllegalStateException("?? null in old list??");

			if(on == nn) {
				/*
				 * Simplest case: same node @ this position - only handle it's children's changes. 
				 */
				if(DEBUG)
					System.out.println("o: @" + oix + "," + nix + ": equal node=" + nn.getActualID());
				oix++;
				nix++;

				//-- The tree here is not dirty -> I will not be re-rendered. Have my attributes changed?
				if(on.hasChangedAttributes())
					ni.addAttrChange(nn); // Add this node's changes to me (the node itself will not be rerendered IF I am not rerendered)
				if(nn instanceof NodeContainer) {
					//-- Node is a container -> handle children.
					NodeContainer nnc = (NodeContainer) nn;
					//					if(nnc.childHasUpdates() ) {					// jal 20081119 does not see it's children have been deleted??
					if(nnc.childHasUpdates() || nnc.getOldChildren() != null) {
						if(DEBUG)
							System.out.println("o: handle child updates for container=" + nnc.getActualID());
						doContainer(ni, nnc);
					}
				}
				nn.clearDelta();
				continue;
			}

			//-- Different nodes..
			if(nn == null) {
				/*
				 * There's an old node but no new'un?? This cannot happen; it indicates that in the OLD
				 * tree there was a node where now there's none. That means it was DELETED; but we already
				 * removed DELETIONS from the tree!
				 */
				throw new IllegalStateException("The impossible has happened (Hell freezeth over?) or there's a huge algorithmic error... My bet's on the second one, so....");
			}

			/*
			 * Nodes differ: we have a MOVE here. Determine what node moved the most.
			 */
			int olddelta = oix - on.m_newNodeIndex; // How much did we move?
			int newdelta = nix - on.m_oldNodeIndex;
			if(olddelta < 0)
				olddelta = -olddelta;
			if(newdelta < 0)
				newdelta = -newdelta;
			if(DEBUG)
				System.out.println("o: @" + oix + "," + nix + ": move, olddelta=" + olddelta + ", newdelta=" + newdelta);

			/*
			 * Decide: if the OLD node is further away than the NEW node then we delete the OLD one.
			 * If the NEW node is further away than the OLD node we INSERT the new one here.
			 */
			if(olddelta > newdelta) {
				//-- Old is further: delete the old node
				ni.addDelete(on);
				if(DEBUG)
					System.out.println("o:   delete old node=" + on.getActualID());
				oix++;
			} else {
				//-- New is further: add the new node && leave old
				if(DEBUG)
					System.out.println("o:   add new node=" + nn.getActualID());
				ni.addAdd(nn.m_origNewIndex, nn);
				nix++;
			}
		}

		while(nix < nlen) {
			NodeBase nn = nl.get(nix++);
			if(DEBUG)
				System.out.println("o:   old exhausted, add new node=" + nn.getActualID());
			ni.addAdd(nn.m_origNewIndex, nn);
		}

		//-- Re-decide again: if the #of adds and deletes is > the size of the new list we re-render
		int ncmd = ni.deleteList.size() + ni.addList.size();
		if((double) ncmd / (double) newl.size() > 0.9) {
			//-- re-render fully.
			ni.setFullRerender();
			if(DEBUG)
				System.out.println("o: final verdict on " + nc.getActualID() + ": #cmds=" + ncmd + " and newsize=" + newl.size() + ", rerender-fully.");
		}

		//-- Add this delta node to it's parent, if needed.
		if(parentInfo.node != nc) { // FIXME THIS TRIES TO ADD BODY TO BODY!?!?!?! 
			if(DEBUG)
				System.out.println("o: nodeInfo add ni=" + ndid(ni) + " to parent=" + ndid(parentInfo));
			parentInfo.addChildChange(ni);
		} else {
			if(DEBUG)
				System.out.println("o: REFUSE to add nodeInfo=" + ndid(ni) + " to parent=" + ndid(parentInfo));
		}
	}

	static private final String ndid(NodeInfo ni) {
		return ni.node == null ? "(ROOT)" : ni.node.getActualID();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	NodeInfo dumpert.									*/
	/*--------------------------------------------------------------*/

	private void dump(NodeInfo ni) throws IOException {
		StringWriter sw = new StringWriter(8192);
		IndentWriter iw = new IndentWriter(sw);
		dump(iw, ni);
		iw.close();
		sw.close();
		System.out.println("---- NodeInfo-render dump -----");
		System.out.println(sw.getBuffer().toString());
	}

	private void dump(IndentWriter iw, NodeInfo ni) throws IOException {
		//-- Node-related info.
		if(ni.node == null)
			iw.print("[(ROOT):(ROOT)]");
		else
			iw.print("[" + ni.node.getTag() + ":" + ni.node.getActualID() + "]");
		if(ni.isFullRender)
			iw.print(" fullRender");
		if(ni.isAdded)
			iw.print(" ADDED");
		iw.println();
		if(ni.deleteList != null && ni.deleteList.size() > 0) {
			iw.inc();
			iw.print("DELETED-NODES: ");
			for(NodeBase nb : ni.deleteList)
				iw.print(nb.getTag() + ":" + nb.getActualID() + " ");
			iw.println();
			iw.dec();
		}

		if(ni.addList != null && ni.addList.size() > 0) {
			iw.inc();
			iw.print("ADDED-NODES: ");
			for(NodeBase nb : ni.addList)
				iw.print(nb.getTag() + ":" + nb.getActualID() + " ");
			iw.println();
			iw.dec();
		}

		if(ni.attrChangeList != null && ni.attrChangeList.size() > 0) {
			iw.inc();
			iw.print("ATTRCHANGED-NODES: ");
			for(NodeBase nb : ni.attrChangeList)
				iw.print(nb.getTag() + ":" + nb.getActualID() + " ");
			iw.println();
			iw.dec();
		}

		if(ni.lowerChanges != null && ni.lowerChanges.size() > 0) {
			iw.inc();
			int i = 0;
			for(NodeInfo lni : ni.lowerChanges) {
				iw.println("LOWER-NODE-CHANGE[" + i + "]");
				iw.inc();
				dump(iw, lni);
				iw.dec();
			}
			iw.dec();
		}
	}
}
