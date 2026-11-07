package by.slava_borisov.lispkitast;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Ast {
    private static final int NIL = -1;

    private final List<Cell> memory = new ArrayList<>();
    private final List<String> tokens = new ArrayList<>();
    private int tokenPosition;

    int createAtom(String value) {
        memory.add(new Cell(value, NIL, NIL));
        return memory.size() - 1;
    }

    int createCons(int head, int tail) {
        memory.add(new Cell(null, head, tail));
        return memory.size() - 1;
    }

    int parse(String input) {
        memory.clear();
        tokens.clear();
        tokenPosition = 0;

        tokenize(input);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Выражение не введено");
        }

        int root = parseExpression();

        if (tokenPosition != tokens.size()) {
            throw new IllegalArgumentException("После выражения обнаружены лишние элементы");
        }

        return root;
    }

    void tokenize(String input) {
        int i = 0;

        while (i < input.length()) {
            char ch = input.charAt(i);

            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            if (ch == '(' || ch == ')') {
                tokens.add(String.valueOf(ch));
                i++;
                continue;
            }

            int start = i;

            while (i < input.length()
                    && !Character.isWhitespace(input.charAt(i))
                    && input.charAt(i) != '('
                    && input.charAt(i) != ')') {
                i++;
            }

            tokens.add(input.substring(start, i));
        }
    }

    int parseExpression() {
        String token = tokens.get(tokenPosition++);

        if (token.equals(")")) {
            throw new IllegalArgumentException("Лишняя закрывающая скобка");
        }

        if (!token.equals("(")) {
            return createAtom(token);
        }

        List<Integer> elements = new ArrayList<>();

        while (tokenPosition < tokens.size() && !tokens.get(tokenPosition).equals(")")) {
            elements.add(parseExpression());
        }

        if (tokenPosition >= tokens.size()) {
            throw new IllegalArgumentException("Отсутствует закрывающая скобка");
        }

        tokenPosition++;

        int list = NIL;

        for (int i = elements.size() - 1; i >= 0; i--) {
            list = createCons(elements.get(i), list);
        }

        return list;
    }

    void checkAddress(int address) {
        if (address < 0 || address >= memory.size()) {
            throw new IllegalArgumentException("Некорректный адрес узла AST");
        }
    }

    List<Integer> getElements(int address) {
        List<Integer> result = new ArrayList<>();

        int current = address;

        while (current != NIL) {
            checkAddress(current);
            Cell cell = memory.get(current);

            result.add(cell.head);
            current = cell.tail;
        }

        return result;
    }

    String restore(int address) {
        if (address == NIL) {
            return "()";
        }

        checkAddress(address);
        Cell cell = memory.get(address);

        if (cell.isAtom()) {
            return cell.atom;
        }

        StringJoiner joiner = new StringJoiner(" ", "(", ")");

        for (int element : getElements(address)) {
            joiner.add(restore(element));
        }

        return joiner.toString();
    }

    void printLogicalTree(int address, String prefix, boolean last) {
        String branch = last ? "└── " : "├── ";

        if (address == NIL) {
            System.out.println(prefix + branch + "ПУСТОЙ СПИСОК");
            return;
        }

        checkAddress(address);
        Cell cell = memory.get(address);

        if (cell.isAtom()) {
            String type = cell.atom.matches("-?\\d+")
                    ? "ЧИСЛО"
                    : "АТОМ";

            System.out.println(prefix + branch + type + ": "
                    + cell.atom + " [адрес " + address + "]");
            return;
        }

        System.out.println(prefix + branch + "СПИСОК [адрес " + address + "]");

        String nextPrefix = prefix + (last ? "    " : "│   ");

        List<Integer> elements = getElements(address);

        if (elements.isEmpty()) {
            System.out.println(nextPrefix + "└── NIL");
            return;
        }

        for (int i = 0; i < elements.size(); i++) {
            printLogicalTree(
                    elements.get(i),
                    nextPrefix,
                    i == elements.size() - 1);
        }
    }

    void printConsTree(int address) {
        if (address == NIL) {
            System.out.println("NIL");
            return;
        }

        checkAddress(address);
        Cell cell = memory.get(address);

        if (cell.isAtom()) {
            System.out.println("ATOM [" + address + "]: " + cell.atom);
            return;
        }

        System.out.println("CONS [" + address + "]");

        printConsChildren(address, "");
    }

    void printConsChildren(int address, String prefix) {
        Cell cell = memory.get(address);

        printPointer("HEAD", cell.head, prefix, false);
        printPointer("TAIL", cell.tail, prefix, true);
    }

    void printPointer(
            String field,
            int address,
            String prefix,
            boolean last) {

        String branch = last ? "└── " : "├── ";

        if (address == NIL) {
            System.out.println(prefix + branch + field + " → NIL");
            return;
        }

        checkAddress(address);
        Cell cell = memory.get(address);

        if (cell.isAtom()) {
            System.out.println(prefix + branch + field + " → ATOM [" + address + "]: " + cell.atom);
            return;
        }

        System.out.println(prefix + branch + field + " → CONS [" + address + "]");

        printConsChildren(
                address,
                prefix + (last ? "    " : "│   "));
    }

    void printMemory() {
        System.out.println("\nЯчейки памяти:");

        if (memory.isEmpty()) {
            System.out.println("Память пуста");
            return;
        }

        for (int i = 0; i < memory.size(); i++) {
            Cell cell = memory.get(i);

            if (cell.isAtom()) {
                System.out.println("[" + i + "] ATOM: " + cell.atom);
            } else {
                System.out.println("[" + i + "] CONS: HEAD="
                        + cell.head
                        + ", TAIL="
                        + (cell.tail == NIL ? "NIL" : cell.tail));
            }
        }
    }
}