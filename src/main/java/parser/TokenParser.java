package parser;

import beans.Token;
import beans.TokenEnum;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by antonskripacev on 23.03.17.
 */
public class TokenParser {
    private int line = 1;
    private int position = 1;
    private LinkedList<Token> list = new LinkedList<Token>();
    private BufferedReader bufferedReader;
    private int prevSymbol = -1;

    public LinkedList<Token> parse(File f) throws Exception {
        bufferedReader = new BufferedReader(new FileReader((f)));
        int symbol = 0;

        while(true){

            if(prevSymbol != -1) {
                symbol = prevSymbol;
                prevSymbol = -1;
            } else {
                symbol = bufferedReader.read();
            }

            if(symbol == -1) {
                break;
            }

            /*
                ПРОПУСКАЕМ СИМВОЛ ПУСТОЙ СТРОКИ
             */
            if(symbol == '\n') {
                line++;
                position = 1;
                continue;
            }

            /*
                ПРОПУСКАЕМ ПРОБЕЛЫ
             */
            if(symbol == ' ') {
                position++;
                continue;
            }

            if(symbol == '\t') {
                position += 4;
                continue;
            }

            /*
                УДАЛЯЕМ КОММЕНТАРИИ
             */
            if(symbol == '@') {
                while((symbol = bufferedReader.read()) != '\n' && symbol != -1) {}
                continue;
            }

            Token token = tryToParse(symbol);

            if(token != null) {
                position++;
                list.addLast(token);
                continue;
            } else {
                System.err.println("unlegal token at line " + line + " position " + position + " " + (char)symbol);
                throw new Exception("unlegal token at line " + line + " position " + position + " " + (char)symbol);
            }
        }

        bufferedReader.close();

        return list;
    }

    @Nullable
    private Token tryToParse(int symbol) throws IOException {
        Token token = null;

        if(Character.isDigit(symbol)) {
            token = tryToParseNumber(symbol);
        } else if(Character.isLetter(symbol)) {
            token = tryToParseID(symbol);
        } else if(symbol == '"') {
            token = tryToParseString(symbol);
        } else {
            token = tryToParseSign(symbol);
        }

        return token;
    }

    @Nullable
    private Token tryToParseString(int symbol) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append((char)symbol);

        symbol = bufferedReader.read();

        while(symbol != '"') {
            if(symbol == -1) return null;

            builder.append((char)symbol);
            symbol = bufferedReader.read();
        }

        builder.append((char)symbol);

        Token token = new Token(line, position);
        token.setType(TokenEnum.STRINGCONST);
        token.setValue(builder.toString());

        return token;
    }

    @Nullable
    private Token tryToParseSign(int symbol) {
        Token t = new Token(line, position);

        TokenEnum e = TokenEnum.getEnumElementByValue(new StringBuilder().append((char)symbol).toString());

        if(e == null) {
            return null;
        } else {
            t.setType(e);
        }

        return t;
    }

    @Nullable
    private Token tryToParseNumber(int symbol) throws IOException {
        StringBuilder builder = new StringBuilder();
        boolean isFirstDigitIsZero = (char)symbol == '0';


        builder.append((char)symbol);

        boolean isDotPass = false;

        if(isFirstDigitIsZero) {
            symbol = bufferedReader.read();
            if(symbol == '.') {
                builder.append((char)symbol);
                isDotPass = true;
            } else if(Character.isDigit((char)symbol)) {
                return null;
            } else {
                Token t = new Token(line, position);
                t.setType(TokenEnum.INTCONST);
                t.setValue(builder.toString());
                prevSymbol = symbol;
                return t;
            }
        }

        while((symbol = bufferedReader.read()) != -1 && (Character.isDigit(symbol) || symbol == '.')) {
            if(symbol == '.' && isDotPass == false) {
                isDotPass = true;
            } else if(symbol == '.' && isDotPass == true) {
                break;
            }

            builder.append((char)symbol);
        }

        if(symbol != -1) {
            prevSymbol = symbol;
        }


        if(builder.charAt(builder.length() - 1) == '.') {
            return null;
        }

        Token t = new Token(line, position);

        if(isDotPass) {
            t.setType(TokenEnum.FLOATCONST);
            t.setValue(builder.toString());
        } else {
            t.setType(TokenEnum.INTCONST);
            t.setValue(builder.toString());
        }


        return t;
    }

    @Nullable
    private Token tryToParseID(int symbol) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append((char)symbol);

        symbol = bufferedReader.read();

        while(symbol != - 1) {
            if(Character.isLetterOrDigit(symbol)) {
                builder.append((char)symbol);
            } else {
                prevSymbol = symbol;
                break;
            }

            symbol = bufferedReader.read();
        }

        Token token = new Token(line, position);
        TokenEnum tokenEnum = TokenEnum.getEnumElementByValue(builder.toString());

        if(tokenEnum != null) {
            if(tokenEnum == TokenEnum.TRUE || tokenEnum == TokenEnum.FALSE) {
                token.setType(TokenEnum.BOOLEANCONST);
                token.setValue(tokenEnum.getValue());
            } else {
                token.setType(tokenEnum);
                token.setValue("");
            }
        } else {
            token.setType(TokenEnum.ID);
            token.setValue(builder.toString());
        }

        return token;
    }
}
