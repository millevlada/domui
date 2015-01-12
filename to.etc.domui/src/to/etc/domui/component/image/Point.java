package to.etc.domui.component.image;

import to.etc.domui.jsmodel.*;

import javax.annotation.*;

@JsClass
public class Point {
	private final int m_x;

	private final int m_y;

	public Point(int x, int y) {
		m_x = x;
		m_y = y;
	}

	public int getX() {
		return m_x;
	}

	public int getY() {
		return m_y;
	}
	@Nonnull
	public Point move(int dx, int dy) {
		return new Point(m_x + dx, m_y + dy);
	}
}