package to.etc.webapp.query;

import java.util.*;

public interface QNodeVisitor {
	public void		visitCriteria(QCriteria<?> qc) throws Exception;
	public void		visitUnaryNode(QUnaryNode n) throws Exception;
	public void		visitLiteral(QLiteral n) throws Exception;
	public void		visitMulti(QMultiNode n) throws Exception;
	public void		visitOrder(QOrder o) throws Exception;
	public void		visitBetween(QBetweenNode n) throws Exception;
	public void 	visitPropertyComparison(QPropertyComparison qPropertyComparison) throws Exception;
	public void		visitUnaryProperty(QUnaryProperty n) throws Exception;
	public void		visitRestrictionsBase(QRestrictionsBase n) throws Exception;
	public void		visitOrderList(List<QOrder> orderlist) throws Exception;
}
