package main;

import intermidiate.CodeGenerator;
import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;
import parser.Ast;
import parser.JsonAstPrinter;
import parser.ParserAst;
import semantic.SemanticAnalyzer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Application {

    /*
    Options (pored run i debug) -> Configuration Edit -> Working directory svoj resources folder
    Ime fajla kao arg komandne linije
     */

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java main.Application <source-file>");
            System.exit(64);
        }
        Path inputFile = null;
        try {
            inputFile = Paths.get(args[0]);
            String code = Files.readString(inputFile);

            System.out.println("----- LEKSICKA ANALIZA -----");
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.scanTokens();
            System.out.println(TokenFormatter.formatList(tokens));

            System.out.println("----- SINTAKSNA ANALIZA -----");
            ParserAst parser = new ParserAst(tokens);
            Ast.Program program = parser.parseProgram();

            JsonAstPrinter printer = new JsonAstPrinter();
            String astJson = printer.print(program);
            Path astOut = Path.of("program_parsed.json");
            Files.writeString(astOut, astJson);
            System.out.println("AST written to: " + astOut);

            System.out.println("----- SEMANTICKA ANALIZA -----");
            SemanticAnalyzer semantic = new SemanticAnalyzer();
            semantic.analyze(program);
            System.out.println("Semantic analysis successful.");
            // TIPIZIRANO AST
            String typedAstJson = printer.print(program);
            Path typedOut = Path.of("program_typed.json");
            Files.writeString(typedOut, typedAstJson);
            System.out.println("Typed AST written to: " + typedOut);

            System.out.println("----- GENERISANJE MEDJUKODA -----");
            CodeGenerator codeGen = new CodeGenerator();
            List<String> intermediateCode = codeGen.generate(program);
            Path codeOut = Path.of("program_generated.txt");
            Files.write(codeOut, intermediateCode);
            System.out.println("Intermediate code written to: " + codeOut);

        }
        catch (FileNotFoundException e) {
            System.err.println("File not found: " + inputFile);
            System.exit(65);

        } catch (IOException e) {
            System.err.println("I/O error while reading " + inputFile + ": " + e.getMessage());
            System.exit(66);
        }
        catch (Exception e) {
            System.err.println("Error: " + escapeVisible(e.getMessage()));
            System.exit(1);
        }
    }

    private static String escapeVisible(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

