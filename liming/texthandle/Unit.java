package liming.texthandle;

public enum Unit {
    KB(1 << 10), MB(1 << 20), GB(1 << 30);

    private int value;

    Unit(int size) {
        value = size;
    }

    public int getValue() {
        return value;
    }
}
