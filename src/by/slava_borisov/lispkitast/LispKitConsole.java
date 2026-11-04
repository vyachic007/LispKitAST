package by.slava_borisov.lispkitast;

import java.util.Scanner;

public class LispKitConsole {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nВведите выражение LispKit (0 — выход): ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Работа завершена.");
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

                System.out.println("\nРезультат обхода дерева:");
                System.out.println(ast.restore(root));

            } catch (IllegalArgumentException e) {
                System.out.println("\nОшибка: " + e.getMessage());
            }
        }

        scanner.close();
    }
}