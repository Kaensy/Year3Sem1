%{
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// If BISON_MODE is not defined, we're in standalone mode
#ifndef BISON_MODE
    // Define token codes for standalone mode
    #define ID 0
    #define CONST 1
    #define INT 2
    #define DOUBLE 3
    #define VOID 4
    #define MAIN 5
    #define COUT 6
    #define CIN 7
    #define WHILE 8
    #define IF 9
    #define ELSE 10
    #define ENDL 11
    #define PLUS 12
    #define MINUS 13
    #define MULT 14
    #define SHIFT_LEFT 15
    #define SHIFT_RIGHT 16
    #define ASSIGN 17
    #define NOT_EQUAL 18
    #define GREATER 19
    #define LESS 20
    #define LESS_EQUAL 21
    #define GREATER_EQUAL 22
    #define EQUAL 23
    #define LEFT_BRACKET 24
    #define RIGHT_BRACKET 25
    #define LEFT_PAREN 26
    #define RIGHT_PAREN 27
    #define LEFT_BRACE 28
    #define RIGHT_BRACE 29
    #define COMMA 30
    #define SEMICOLON 31
    #define CONST_STRING 32
    #define CONST_POSITIVE 33
#else
    #include "parser.tab.h"
#endif

// Structure for symbol table
struct SymbolEntry {
    char* lexeme;
    int index;
};

// Global variables
struct SymbolEntry symbol_table[1000];
int symbol_table_size = 0;
FILE* fip_file;
FILE* ts_file;

// Function declarations
int insert_symbol(char* lexeme);
#ifndef BISON_MODE
    void write_to_fip(char* token, int code, int ts_position);
#endif
%}

%option yylineno
%option noyywrap

/* Definitions section */
DIGIT       [0-9]
LETTER      [a-zA-Z]
ID          {LETTER}({LETTER}|{DIGIT}|"_")*
WHITESPACE  [ \t\r]+
CONST_INT   "0"|[1-9]{DIGIT}*
CONST_REAL  {CONST_INT}"."{DIGIT}+
CONST_STR   \"[^\"]*\"

%%
{WHITESPACE}    { /* Ignore whitespace */ }
\n              { /* Handled by yylineno */ }

"int"           {
#ifdef BISON_MODE
    return INT;
#else
    write_to_fip(yytext, INT, -1);
#endif
}

"double"        {
#ifdef BISON_MODE
    return DOUBLE;
#else
    write_to_fip(yytext, DOUBLE, -1);
#endif
}

"main"          {
#ifdef BISON_MODE
    return MAIN;
#else
    write_to_fip(yytext, MAIN, -1);
#endif
}

"cout"          {
#ifdef BISON_MODE
    return COUT;
#else
    write_to_fip(yytext, COUT, -1);
#endif
}

"cin"           {
#ifdef BISON_MODE
    return CIN;
#else
    write_to_fip(yytext, CIN, -1);
#endif
}

"endl"          {
#ifdef BISON_MODE
    return ENDL;
#else
    write_to_fip(yytext, ENDL, -1);
#endif
}

"+"             {
#ifdef BISON_MODE
    return PLUS;
#else
    write_to_fip(yytext, PLUS, -1);
#endif
}

"-"             {
#ifdef BISON_MODE
    return MINUS;
#else
    write_to_fip(yytext, MINUS, -1);
#endif
}

"*"             {
#ifdef BISON_MODE
    return MULT;
#else
    write_to_fip(yytext, MULT, -1);
#endif
}

"<<"            {
#ifdef BISON_MODE
    return SHIFT_LEFT;
#else
    write_to_fip(yytext, SHIFT_LEFT, -1);
#endif
}

">>"            {
#ifdef BISON_MODE
    return SHIFT_RIGHT;
#else
    write_to_fip(yytext, SHIFT_RIGHT, -1);
#endif
}

"="             {
#ifdef BISON_MODE
    return ASSIGN;
#else
    write_to_fip(yytext, ASSIGN, -1);
#endif
}

"("             {
#ifdef BISON_MODE
    return LEFT_PAREN;
#else
    write_to_fip(yytext, LEFT_PAREN, -1);
#endif
}

")"             {
#ifdef BISON_MODE
    return RIGHT_PAREN;
#else
    write_to_fip(yytext, RIGHT_PAREN, -1);
#endif
}

"{"             {
#ifdef BISON_MODE
    return LEFT_BRACE;
#else
    write_to_fip(yytext, LEFT_BRACE, -1);
#endif
}

"}"             {
#ifdef BISON_MODE
    return RIGHT_BRACE;
#else
    write_to_fip(yytext, RIGHT_BRACE, -1);
#endif
}

","             {
#ifdef BISON_MODE
    return COMMA;
#else
    write_to_fip(yytext, COMMA, -1);
#endif
}

";"             {
#ifdef BISON_MODE
    return SEMICOLON;
#else
    write_to_fip(yytext, SEMICOLON, -1);
#endif
}

{ID}            { 
    int pos = insert_symbol(yytext);
#ifdef BISON_MODE
    yylval.strval = strdup(yytext);
    return ID;
#else
    write_to_fip(yytext, ID, pos);
#endif
}

{CONST_INT}     { 
    int pos = insert_symbol(yytext);
#ifdef BISON_MODE
    yylval.strval = strdup(yytext);
    if(atoi(yytext) > 0) {
        return CONST_POSITIVE;
    }
    return CONST;
#else
    if(atoi(yytext) > 0) {
        write_to_fip(yytext, CONST_POSITIVE, pos);
    } else {
        write_to_fip(yytext, CONST, pos);
    }
#endif
}

{CONST_REAL}    { 
    int pos = insert_symbol(yytext);
#ifdef BISON_MODE
    yylval.strval = strdup(yytext);
    return CONST;
#else
    write_to_fip(yytext, CONST, pos);
#endif
}

{CONST_STR}     { 
    int pos = insert_symbol(yytext);
#ifdef BISON_MODE
    yylval.strval = strdup(yytext);
    return CONST_STRING;
#else
    write_to_fip(yytext, CONST_STRING, pos);
#endif
}

.               { printf("Error at line %d: Unrecognized token '%s'\n", yylineno, yytext); }

%%

int insert_symbol(char* lexeme) {
    // Check if symbol already exists
    for(int i = 0; i < symbol_table_size; i++) {
        if(strcmp(symbol_table[i].lexeme, lexeme) == 0) {
            return symbol_table[i].index;
        }
    }
    
    // Add new symbol
    symbol_table[symbol_table_size].lexeme = strdup(lexeme);
    symbol_table[symbol_table_size].index = symbol_table_size + 1;
    symbol_table_size++;
    
    return symbol_table_size;
}

#ifndef BISON_MODE
void write_to_fip(char* token, int code, int ts_position) {
    fprintf(fip_file, "%-15s| %-8d| %-12d\n", token, code, ts_position);
}

int main(int argc, char* argv[]) {
    if(argc != 2) {
        printf("Usage: %s input_file\n", argv[0]);
        return 1;
    }
    
    yyin = fopen(argv[1], "r");
    if(!yyin) {
        printf("Cannot open input file\n");
        return 1;
    }
    
    fip_file = fopen("fip_output.txt", "w");
    ts_file = fopen("ts_output.txt", "w");
    
    fprintf(fip_file, "Token          | Atom ID | TS Position\n");
    fprintf(fip_file, "----------------------------------------\n");
    
    fprintf(ts_file, "Symbol         | Index\n");
    fprintf(ts_file, "--------------------\n");
    
    yylex();
    
    for(int i = 0; i < symbol_table_size; i++) {
        fprintf(ts_file, "%-15s| %d\n", symbol_table[i].lexeme, symbol_table[i].index);
    }
    
    fclose(yyin);
    fclose(fip_file);
    fclose(ts_file);
    
    return 0;
}
#endif