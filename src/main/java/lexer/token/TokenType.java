package lexer.token;

public enum TokenType {

    // OSNOVNO:
    COMMA, ASSIGN, EOF,
    IDENTIFICATOR, SEPARATOR, INC, DEC,

    // ZAGRADE:
    LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,

    // TIPOVI PODATAKA:
    BOOLEAN, // redstone
    INT, // gold
    DOUBLE, // diamond
    LONG, // emerald
    CHAR, // iron
    STRING, // chain
    ARRAY, // chest

    // TIPOVI LITERALA:
    INT_LIT,
    DOUBLE_LIT,
    LONG_LIT,
    CHAR_LIT,
    STRING_LIT,
    ARRAY_LIT,

    // BOOLEAN
    TRUE, FALSE,

    // USLOVI I PETLJE:
    IF, // dig
    ELSEIF, // deeper
    ELSE, // bedrock
    FOR, // craft
    WHILE, // build
    DO, // do

    // STRUKTURA PROGRAMA:
    FUNCTION, RETURN,

    // ARITMETIKA:
    ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT, CARET,

    // POREDJENJE:
    LT, LE, GT, GE, EQ, NEQ,

    // STRUKTURA PROGRAMA:
    SCAN, // drop
    PRINT, // collect

    // LOGICKI OPERATORI
    AND, OR, NOT, // npr !powered = unpowered

    // BITOVNE
    BIT_LSHIFT, BIT_RSHIFT, BIT_AND, BIT_OR,

    // TERNARNI
    TQUESTION, TSEMICOLON,

    // KLASE
    CLASS, EXTENDS



}
