package by.slava_borisov.lispkitast;

class Cell {
    final String atom;
    final int head;
    final int tail;

    Cell(String atom, int head, int tail) {
        this.atom = atom;
        this.head = head;
        this.tail = tail;
    }

    boolean isAtom() {
        return atom != null;
    }
}