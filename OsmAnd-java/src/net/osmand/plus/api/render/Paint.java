package net.osmand.plus.api.render;


public interface Paint {
	public enum Style {
        FILL            (0),
        STROKE          (1),
        FILL_AND_STROKE (2);
        Style(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public enum Cap {
        BUTT    (0),
        ROUND   (1),
        SQUARE  (2);
        private Cap(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public enum Join {
        MITER   (0),
        ROUND   (1),
        BEVEL   (2);
        private Join(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public enum Align {
        LEFT    (0),
        CENTER  (1),
        RIGHT   (2);
        private Align(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

	void setColor(int color);

	int getColor();

	void setTextAlign(Align align);

	void setStrokeWidth(float width);

	void setAntiAlias(boolean flag);

	void setStyle(Style style);
}
