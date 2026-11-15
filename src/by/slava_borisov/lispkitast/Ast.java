package by.slava_borisov.lispkitast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

        validateExpression(root);

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
        if (tokenPosition >= tokens.size()) {
            throw new IllegalArgumentException("Неожиданный конец выражения");
        }

        String token = tokens.get(tokenPosition++);

        if (token.equals(")")) {
            throw new IllegalArgumentException("Лишняя закрывающая скобка");
        }

        if (!token.equals("(")) {
            if (!token.matches("-?\\d+|[A-Za-z][A-Za-z0-9]*")) {
                throw new IllegalArgumentException("Недопустимый атом: " + token);
            }

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

    void validateExpression(int address) {
        if (address == NIL) {
            return;
        }

        Cell cell = memory.get(address);

        if (cell.isAtom()) {
            return;
        }

        List<Integer> elements = getElements(address);

        if (elements.isEmpty()) {
            return;
        }

        int first = elements.get(0);

        String operator = first != NIL && memory.get(first).isAtom()
                ? memory.get(first).atom.toUpperCase(Locale.ROOT)
                : null;

        if (operator == null) {
            validateFunctionCall(elements);
            return;
        }

        switch (operator) {
            case "QUOTE":
                requireSize(elements, 2, operator);
                return;

            case "ADD":
            case "SUB":
            case "MUL":
            case "MULT":
            case "DIV":
            case "DIVE":
            case "REM":
            case "EQUAL":
            case "LE":
            case "LEQ":
            case "CONS":
                requireSize(elements, 3, operator);
                break;

            case "CAR":
            case "CDR":
            case "ATOM":
                requireSize(elements, 2, operator);
                break;

            case "COND":
                requireSize(elements, 4, operator);
                break;

            case "LAMBDA":
                validateLambda(elements);
                return;

            case "LET":
                validateLet(elements, false);
                return;

            case "LETREC":
                validateLet(elements, true);
                return;

            default:
                validateFunctionCall(elements);
                return;
        }

        for (int i = 1; i < elements.size(); i++) {
            validateExpression(elements.get(i));
        }
    }

    void validateFunctionCall(List<Integer> elements) {
        int function = elements.get(0);

        if (function == NIL
                || !memory.get(function).isAtom()
                || !isIdentifier(memory.get(function).atom)) {
            throw new IllegalArgumentException(
                    "Имя вызываемой функции должно быть идентификатором");
        }

        for (int i = 1; i < elements.size(); i++) {
            validateExpression(elements.get(i));
        }
    }

    void validateLambda(List<Integer> elements) {
        requireSize(elements, 3, "LAMBDA");

        int parameters = elements.get(1);

        if (parameters != NIL) {
            if (memory.get(parameters).isAtom()) {
                throw new IllegalArgumentException(
                        "LAMBDA: ожидается список параметров");
            }

            for (int parameter : getElements(parameters)) {
                if (parameter == NIL
                        || !memory.get(parameter).isAtom()
                        || !isIdentifier(memory.get(parameter).atom)
                        || isReservedWord(memory.get(parameter).atom)) {
                    throw new IllegalArgumentException(
                            "LAMBDA: параметры должны быть идентификаторами");
                }
            }
        }

        validateExpression(elements.get(2));
    }

    void validateLet(List<Integer> elements, boolean recursive) {
        String name = recursive ? "LETREC" : "LET";

        if (elements.size() < 2) {
            throw new IllegalArgumentException(
                    name + ": отсутствует вычисляемое выражение");
        }

        validateExpression(elements.get(1));

        for (int i = 2; i < elements.size(); i++) {
            validateBinding(elements.get(i), recursive, name);
        }
    }

    void validateBinding(int address, boolean recursive, String name) {
        if (address == NIL || memory.get(address).isAtom()) {
            throw new IllegalArgumentException(
                    name + ": ожидается пара (имя выражение)");
        }

        List<Integer> binding = getElements(address);

        requireSize(binding, 2, name + ": объявление");

        int identifier = binding.get(0);
        int value = binding.get(1);

        if (identifier == NIL
                || !memory.get(identifier).isAtom()
                || !isIdentifier(memory.get(identifier).atom)
                || isReservedWord(memory.get(identifier).atom)) {
            throw new IllegalArgumentException(
                    name + ": имя должно быть допустимым идентификатором");
        }

        if (recursive && !isLambda(value)) {
            throw new IllegalArgumentException(
                    "LETREC: определение функции должно содержать LAMBDA");
        }

        validateExpression(value);
    }

    boolean isLambda(int address) {
        if (address == NIL || memory.get(address).isAtom()) {
            return false;
        }

        List<Integer> elements = getElements(address);

        if (elements.isEmpty()) {
            return false;
        }

        int first = elements.get(0);

        return first != NIL
                && memory.get(first).isAtom()
                && memory.get(first).atom.equalsIgnoreCase("LAMBDA");
    }

    boolean isIdentifier(String value) {
        return value.matches("[A-Za-z][A-Za-z0-9]*");
    }

    boolean isReservedWord(String value) {
        switch (value.toUpperCase(Locale.ROOT)) {
            case "QUOTE":
            case "CAR":
            case "CDR":
            case "CONS":
            case "ATOM":
            case "EQUAL":
            case "ADD":
            case "SUB":
            case "MUL":
            case "MULT":
            case "DIV":
            case "DIVE":
            case "REM":
            case "LE":
            case "LEQ":
            case "COND":
            case "LAMBDA":
            case "LET":
            case "LETREC":
                return true;
            default:
                return false;
        }
    }

    void requireSize(List<Integer> elements, int expected, String name) {
        if (elements.size() < expected) {
            throw new IllegalArgumentException(
                    name + ": недостаточно аргументов");
        }

        if (elements.size() > expected) {
            throw new IllegalArgumentException(
                    name + ": лишние аргументы");
        }
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