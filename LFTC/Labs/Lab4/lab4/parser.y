%{
#include <stdio.h>
#include <stdlib.h>

void yyerror(const char *s);
extern int yylex();
extern int yylineno;
extern FILE *yyin;
%}

%union {
    char* strval;
}

/* Token declarations */
%token <strval> ID CONST CONST_STRING CONST_POSITIVE
%token INT DOUBLE VOID MAIN
%token COUT CIN WHILE IF ELSE
%token M_PI ENDL
%token PLUS MINUS MULT
%token SHIFT_LEFT SHIFT_RIGHT
%token ASSIGN
%token NOT_EQUAL GREATER LESS LESS_EQUAL GREATER_EQUAL EQUAL
%token LEFT_BRACKET RIGHT_BRACKET
%token LEFT_PAREN RIGHT_PAREN
%token LEFT_BRACE RIGHT_BRACE
%token COMMA SEMICOLON

/* Operator precedence */
%left PLUS MINUS
%left MULT

%%

program         : INT MAIN LEFT_PAREN RIGHT_PAREN LEFT_BRACE declarations instructions RIGHT_BRACE
                ;

declarations    : list_decl
                | /* empty */
                ;

list_decl      : decl SEMICOLON
                | list_decl decl SEMICOLON
                ;

decl           : type list_def
                ;

type           : INT
                | DOUBLE
                ;

list_def       : def
                | list_def COMMA def
                ;

def            : ID
                | ID ASSIGN numeric_const
                | ID LEFT_BRACKET CONST_POSITIVE RIGHT_BRACKET
                ;

instructions   : list_instr
                | /* empty */
                ;

list_instr     : instr SEMICOLON
                | list_instr instr SEMICOLON
                ;

instr          : assign
                | instr_if
                | instr_loop
                | read
                | write
                ;

assign         : ID ASSIGN var_list
                | array_element ASSIGN var_list
                ;

array_element  : ID LEFT_BRACKET array_index RIGHT_BRACKET
                ;

array_index    : CONST_POSITIVE
                | ID
                ;

var_list       : var
                | var_list operator_arithm var
                ;

var            : ID 
                | numeric_const
                | array_element
                ;

numeric_const  : CONST
                | CONST_POSITIVE
                ;

operator_arithm : PLUS
                | MINUS
                | MULT
                ;

instr_if       : IF LEFT_PAREN condition RIGHT_PAREN LEFT_BRACE list_instr RIGHT_BRACE
                | IF LEFT_PAREN condition RIGHT_PAREN LEFT_BRACE list_instr RIGHT_BRACE 
                  ELSE LEFT_BRACE list_instr RIGHT_BRACE
                ;

condition      : var operator_rel var
                ;

operator_rel   : NOT_EQUAL
                | GREATER
                | LESS
                | LESS_EQUAL
                | GREATER_EQUAL
                | EQUAL
                ;

instr_loop     : WHILE LEFT_PAREN condition RIGHT_PAREN LEFT_BRACE list_instr RIGHT_BRACE
                ;

read           : CIN SHIFT_RIGHT ID
                ;

write          : COUT writing_list
                ;

writing_list   : writing
                | writing_list writing
                ;

writing        : SHIFT_LEFT writeable
                ;

writeable      : var
                | ENDL
                | CONST_STRING
                ;

%%

void yyerror(const char *s) {
    fprintf(stderr, "Syntax error at line %d: %s\n", yylineno, s);
}

int main(int argc, char **argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s input_file\n", argv[0]);
        return 1;
    }
    
    FILE *input = fopen(argv[1], "r");
    if (!input) {
        fprintf(stderr, "Cannot open input file %s\n", argv[1]);
        return 1;
    }
    
    yyin = input;
    int result = yyparse();
    fclose(input);
    
    if (result == 0) {
        printf("Parsing completed successfully.\n");
    }
    
    return result;
}