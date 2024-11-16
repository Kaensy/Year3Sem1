%{
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// Structure for symbol table
struct SymbolEntry {
    char* lexeme;
    int index;
};

// Global variables
int line_number = 1;
int atom_count = 0;
struct SymbolEntry symbol_table[1000];
int symbol_table_size = 0;
FILE* fip_file;
FILE* ts_file;

// Function declarations
int insert_symbol(char* lexeme);
void write_to_fip(char* token, int code, int ts_position);
%}

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
\n              { line_number++; }

"int"           { write_to_fip(yytext, 2, -1); }
"double"        { write_to_fip(yytext, 3, -1); }
"void"          { write_to_fip(yytext, 4, -1); }
"main"          { write_to_fip(yytext, 5, -1); }
"cout"          { write_to_fip(yytext, 6, -1); }
"cin"           { write_to_fip(yytext, 7, -1); }
"while"         { write_to_fip(yytext, 8, -1); }
"if"            { write_to_fip(yytext, 9, -1); }
"else"          { write_to_fip(yytext, 10, -1); }
"endl"          { write_to_fip(yytext, 11, -1); }

"+"             { write_to_fip(yytext, 12, -1); }
"-"             { write_to_fip(yytext, 13, -1); }
"*"             { write_to_fip(yytext, 14, -1); }
"<<"            { write_to_fip(yytext, 15, -1); }
">>"            { write_to_fip(yytext, 16, -1); }
"="             { write_to_fip(yytext, 17, -1); }
"!="            { write_to_fip(yytext, 18, -1); }
">"             { write_to_fip(yytext, 19, -1); }
"<"             { write_to_fip(yytext, 20, -1); }
"<="            { write_to_fip(yytext, 21, -1); }
">="            { write_to_fip(yytext, 22, -1); }
"=="            { write_to_fip(yytext, 23, -1); }
"["             { write_to_fip(yytext, 24, -1); }
"]"             { write_to_fip(yytext, 25, -1); }

"("             { write_to_fip(yytext, 26, -1); }
")"             { write_to_fip(yytext, 27, -1); }
"{"             { write_to_fip(yytext, 28, -1); }
"}"             { write_to_fip(yytext, 29, -1); }
","             { write_to_fip(yytext, 30, -1); }
";"             { write_to_fip(yytext, 31, -1); }

{ID}            { 
    int pos = insert_symbol(yytext);
    write_to_fip(yytext, 0, pos); 
}

{CONST_INT}|{CONST_REAL}|{CONST_STR} { 
    int pos = insert_symbol(yytext);
    write_to_fip(yytext, 1, pos); 
}

.               { printf("Error at line %d: Unrecognized token '%s'\n", line_number, yytext); }

%%

int yywrap() {
    return 1;
}

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

void write_to_fip(char* token, int code, int ts_position) {
    fprintf(fip_file, "%-15s| %-8d| %-12d\n", token, code, ts_position);
}

int main(int argc, char* argv[]) {
    if(argc != 2) {
        printf("Usage: %s input_file\n", argv[0]);
        return 1;
    }
    
    // Open input file
    yyin = fopen(argv[1], "r");
    if(!yyin) {
        printf("Cannot open input file\n");
        return 1;
    }
    
    // Open output files
    fip_file = fopen("fip_output.txt", "w");
    ts_file = fopen("ts_output.txt", "w");
    
    // Write headers
    fprintf(fip_file, "Token          | Atom ID | TS Position\n");
    fprintf(fip_file, "----------------------------------------\n");
    
    fprintf(ts_file, "Symbol         | Index\n");
    fprintf(ts_file, "--------------------\n");
    
    // Process input
    yylex();
    
    // Write symbol table
    for(int i = 0; i < symbol_table_size; i++) {
        fprintf(ts_file, "%-15s| %d\n", symbol_table[i].lexeme, symbol_table[i].index);
    }
    
    // Close files
    fclose(yyin);
    fclose(fip_file);
    fclose(ts_file);
    
    return 0;
}
