package by.slava_borisov.lispkitast;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

public class Main {

    static final int NIL = -1;

    static class Cell {
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

    static class Ast {
        private final List<Cell> memory = new ArrayList<>();
        private final List<String> tokens = new ArrayList<>();
        private int position;

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
            position = 0;

            tokenize(input);

            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("Выражение не введено");
            }

            int root = parseExpression();

            if (position != tokens.size()) {
                throw new IllegalArgumentException(
                        "После выражения обнаружены лишние элементы"
                );
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
            if (position >= tokens.size()) {
                throw new IllegalArgumentException(
                        "Неожиданный конец выражения"
                );
            }

            String token = tokens.get(position++);

            if (token.equals(")")) {
                throw new IllegalArgumentException(
                        "Лишняя закрывающая скобка"
                );
            }

            if (!token.equals("(")) {
                return createAtom(token);
            }

            List<Integer> elements = new ArrayList<>();

            while (position < tokens.size()
                    && !tokens.get(position).equals(")")) {
                elements.add(parseExpression());
            }

            if (position >= tokens.size()) {
                throw new IllegalArgumentException(
                        "Отсутствует закрывающая скобка"
                );
            }

            position++;

            int list = NIL;

            for (int i = elements.size() - 1; i >= 0; i--) {
                list = createCons(elements.get(i), list);
            }

            return list;
        }

        List<Integer> getElements(int address) {
            List<Integer> elements = new ArrayList<>();

            int current = address;

            while (current != NIL) {
                Cell cell = memory.get(current);

                if (cell.isAtom()) {
                    throw new IllegalArgumentException(
                            "Нарушена структура списка"
                    );
                }

                elements.add(cell.head);
                current = cell.tail;
            }

            return elements;
        }

        String restore(int address) {
            if (address == NIL) {
                return "()";
            }

            Cell cell = memory.get(address);

            if (cell.isAtom()) {
                return cell.atom;
            }

            StringJoiner result = new StringJoiner(" ", "(", ")");

            for (int element : getElements(address)) {
                result.add(restore(element));
            }

            return result.toString();
        }

        void printLogicalTree(int address, String prefix, boolean last) {
            String branch = last ? "└── " : "├── ";

            if (address == NIL) {
                System.out.println(prefix + branch + "ПУСТОЙ СПИСОК");
                return;
            }

            Cell cell = memory.get(address);

            if (cell.isAtom()) {
                String type = cell.atom.matches("-?\\d+")
                        ? "ЧИСЛО"
                        : "АТОМ";

                System.out.println(
                        prefix + branch + type + ": " + cell.atom
                                + " [адрес " + address + "]"
                );
                return;
            }

            System.out.println(
                    prefix + branch + "СПИСОК [адрес " + address + "]"
            );

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
                        i == elements.size() - 1
                );
            }
        }

        void printConsTree(int address) {
            if (address == NIL) {
                System.out.println("NIL");
                return;
            }

            Cell root = memory.get(address);

            if (root.isAtom()) {
                System.out.println(
                        "ATOM [" + address + "]: " + root.atom
                );
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
                boolean last
        ) {
            String branch = last ? "└── " : "├── ";

            if (address == NIL) {
                System.out.println(
                        prefix + branch + field + " → NIL"
                );
                return;
            }

            Cell cell = memory.get(address);

            if (cell.isAtom()) {
                System.out.println(
                        prefix + branch + field
                                + " → ATOM [" + address + "]: " + cell.atom
                );
                return;
            }

            System.out.println(
                    prefix + branch + field
                            + " → CONS [" + address + "]"
            );

            String nextPrefix = prefix + (last ? "    " : "│   ");
            printConsChildren(address, nextPrefix);
        }

        void printMemory() {
            System.out.println("\nЯчейки памяти:");

            for (int i = 0; i < memory.size(); i++) {
                Cell cell = memory.get(i);

                if (cell.isAtom()) {
                    System.out.println(
                            "[" + i + "] ATOM: " + cell.atom
                    );
                } else {
                    System.out.println(
                            "[" + i + "] CONS: HEAD="
                                    + cell.head
                                    + ", TAIL="
                                    + (cell.tail == NIL ? "NIL" : cell.tail)
                    );
                }
            }
        }

        String getAtom(int address) {
            if (address == NIL) {
                return null;
            }

            Cell cell = memory.get(address);
            return cell.isAtom() ? cell.atom : null;
        }

        void printEnvironments(int root) {
            System.out.println("\nОкружения переменных:");
            analyze(root, new ArrayList<>());
        }

        void analyze(int address, List<List<String>> frames) {
            if (address == NIL) {
                return;
            }

            Cell cell = memory.get(address);

            if (cell.isAtom()) {
                findVariable(cell.atom, frames);
                return;
            }

            List<Integer> elements = getElements(address);

            if (elements.isEmpty()) {
                return;
            }

            String operation = getAtom(elements.get(0));

            if (("LET".equalsIgnoreCase(operation)
                    || "LETREC".equalsIgnoreCase(operation))
                    && elements.size() >= 3) {
                analyzeLet(elements, frames, operation);
                return;
            }

            if ("QUOTE".equalsIgnoreCase(operation)) {
                return;
            }

            for (int element : elements) {
                analyze(element, frames);
            }
        }

        void analyzeLet(
                List<Integer> elements,
                List<List<String>> frames,
                String operation
        ) {
            List<String> names = new ArrayList<>();
            List<Integer> values = new ArrayList<>();

            for (int i = 2; i < elements.size(); i++) {
                int bindingAddress = elements.get(i);

                if (bindingAddress == NIL
                        || memory.get(bindingAddress).isAtom()) {
                    throw new IllegalArgumentException(
                            "Некорректное объявление переменной"
                    );
                }

                List<Integer> binding = getElements(bindingAddress);

                if (binding.size() != 2) {
                    throw new IllegalArgumentException(
                            "Объявление должно содержать имя и значение"
                    );
                }

                String name = getAtom(binding.get(0));

                if (name == null) {
                    throw new IllegalArgumentException(
                            "Имя переменной должно быть атомом"
                    );
                }

                if (names.contains(name)) {
                    throw new IllegalArgumentException(
                            "Повторное объявление переменной: " + name
                    );
                }

                names.add(name);
                values.add(binding.get(1));
            }

            System.out.println("\nОператор: " + operation);
            System.out.println("n = " + names);

            List<String> valueTexts = new ArrayList<>();

            for (int value : values) {
                valueTexts.add(restore(value));
            }

            System.out.println("v = " + valueTexts);

            List<List<String>> localFrames = new ArrayList<>();
            localFrames.add(names);
            localFrames.addAll(frames);

            System.out.println("Позиции переменных:");

            for (int i = 0; i < names.size(); i++) {
                System.out.println(
                        names.get(i) + " → (b=0, m=" + i + ")"
                );
            }

            boolean recursive = operation.equalsIgnoreCase("LETREC");

            for (int value : values) {
                analyze(value, recursive ? localFrames : frames);
            }

            System.out.println("Переменные в теле выражения:");
            analyze(elements.get(1), localFrames);
        }

        void findVariable(String name, List<List<String>> frames) {
            for (int b = 0; b < frames.size(); b++) {
                int m = frames.get(b).indexOf(name);

                if (m >= 0) {
                    System.out.println(
                            name + " → (b=" + b + ", m=" + m + ")"
                    );
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nВведите выражение LispKit (0 — выход): ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                break;
            }

            try {
                Ast ast = new Ast();
                int root = ast.parse(input);

                System.out.println("\nИсходное выражение:");
                System.out.println(input);

                System.out.println("\nЛогическое АСД:");
                ast.printLogicalTree(root, "", true);

                System.out.println("\nПолное дерево CONS-ячеек:");
                System.out.println("Корневой адрес: " + root);
                ast.printConsTree(root);

                ast.printMemory();
                ast.printEnvironments(root);

                System.out.println("\nРезультат обхода дерева:");
                System.out.println(ast.restore(root));

            } catch (IllegalArgumentException e) {
                System.out.println("\nОшибка: " + e.getMessage());
            }
        }
    }
}