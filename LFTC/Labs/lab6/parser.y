%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern int yylex();
extern int yylineno;
extern char* yytext;
extern FILE* yyin;
void yyerror(const char* s);

// Symbol table structure
#define MAX_SYMBOLS 100
struct {
    char* name;
    int offset;
    int initialized;
} symbol_table[MAX_SYMBOLS];
int symbol_count = 0;

// Assembly code generation
FILE* asm_file;
int label_counter = 0;
int current_temp = -1;

// Function declarations
int get_variable_offset(char* name);
void add_variable(char* name);
char* new_label();
%}

%union {
    int num;
    char* strval;
}

/* Token declarations */
%token PROGRAM VAR END READ WRITE
%token ASSIGN PLUS MINUS MULT LPAREN RPAREN SEMICOLON COMMA
%token <strval> IDENTIFIER
%token <num> NUMBER

/* Type declarations for non-terminals */
%type <strval> identifier

%%

program         : PROGRAM identifier SEMICOLON 
                 {
                     fprintf(asm_file, "section .data\n");
                     fprintf(asm_file, "    fmt_input db \"%%lld\", 0\n");
                     fprintf(asm_file, "    fmt_output db \"%%lld\", 10, 0\n");
                     fprintf(asm_file, "section .bss\n");
                 }
                 declarations 
                 {
                     fprintf(asm_file, "section .text\n");
                     fprintf(asm_file, "    global main\n");
                     fprintf(asm_file, "    extern printf\n");
                     fprintf(asm_file, "    extern scanf\n");
                     fprintf(asm_file, "main:\n");
                     fprintf(asm_file, "    push rbp\n");
                     fprintf(asm_file, "    mov rbp, rsp\n");
                     fprintf(asm_file, "    sub rsp, 32\n");
                 }
                 instructions END
                 {
                     fprintf(asm_file, "    xor eax, eax\n");
                     fprintf(asm_file, "    leave\n");
                     fprintf(asm_file, "    ret\n");
                 }
                ;

declarations    : VAR id_list SEMICOLON
                ;

id_list         : identifier
                 {
                     add_variable($1);
                     fprintf(asm_file, "    %s resq 1\n", $1);
                 }
                | id_list COMMA identifier
                 {
                     add_variable($3);
                     fprintf(asm_file, "    %s resq 1\n", $3);
                 }
                ;

instructions    : instruction
                | instructions instruction
                ;

instruction     : assign SEMICOLON
                | read SEMICOLON
                | write SEMICOLON
                ;

assign          : identifier ASSIGN expression
                 {
                     fprintf(asm_file, "    mov [%s], rax\n", $1);
                 }
                ;

read            : READ identifier
                 {
                     fprintf(asm_file, "    lea rsi, [%s]\n", $2);
                     fprintf(asm_file, "    lea rdi, [fmt_input]\n");
                     fprintf(asm_file, "    xor eax, eax\n");
                     fprintf(asm_file, "    call scanf\n");
                 }
                ;

write           : WRITE expression
                 {
                     fprintf(asm_file, "    mov rsi, rax\n");
                     fprintf(asm_file, "    lea rdi, [fmt_output]\n");
                     fprintf(asm_file, "    xor eax, eax\n");
                     fprintf(asm_file, "    call printf\n");
                 }
                ;

expression      : term
                | expression PLUS term
                 {
                     fprintf(asm_file, "    push rax\n");
                     fprintf(asm_file, "    mov rbx, rax\n");
                     fprintf(asm_file, "    pop rax\n");
                     fprintf(asm_file, "    add rax, rbx\n");
                 }
                | expression MINUS term
                 {
                     fprintf(asm_file, "    mov rbx, rax\n");
                     fprintf(asm_file, "    pop rax\n");
                     fprintf(asm_file, "    sub rax, rbx\n");
                 }
                ;

term            : factor
                | term MULT factor
                 {
                     fprintf(asm_file, "    mov rbx, rax\n");
                     fprintf(asm_file, "    pop rax\n");
                     fprintf(asm_file, "    imul rax, rbx\n");
                 }
                ;

factor          : identifier
                 {
                     fprintf(asm_file, "    mov rax, [%s]\n", $1);
                     fprintf(asm_file, "    push rax\n");
                 }
                | NUMBER
                 {
                     fprintf(asm_file, "    mov rax, %d\n", $1);
                     fprintf(asm_file, "    push rax\n");
                 }
                | LPAREN expression RPAREN
                ;

identifier      : IDENTIFIER
                 {
                     $$ = $1;
                 }
                ;

%%

void yyerror(const char* s) {
    fprintf(stderr, "Error at line %d: %s\n", yylineno, s);
}

int get_variable_offset(char* name) {
    for(int i = 0; i < symbol_count; i++) {
        if(strcmp(symbol_table[i].name, name) == 0) {
            return symbol_table[i].offset;
        }
    }
    return -1;
}

void add_variable(char* name) {
    if(symbol_count >= MAX_SYMBOLS) {
        fprintf(stderr, "Symbol table full\n");
        exit(1);
    }
    
    // Check if variable already exists
    for(int i = 0; i < symbol_count; i++) {
        if(strcmp(symbol_table[i].name, name) == 0) {
            fprintf(stderr, "Error: Variable %s already declared\n", name);
            exit(1);
        }
    }
    
    symbol_table[symbol_count].name = strdup(name);
    symbol_table[symbol_count].offset = symbol_count * 8;
    symbol_table[symbol_count].initialized = 0;
    symbol_count++;
}

char* new_label() {
    char* label = malloc(20);
    sprintf(label, "L%d", label_counter++);
    return label;
}

int main(int argc, char** argv) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s input_file\n", argv[0]);
        return 1;
    }

    yyin = fopen(argv[1], "r");
    if (!yyin) {
        fprintf(stderr, "Cannot open input file %s\n", argv[1]);
        return 1;
    }

    char asm_filename[256];
    snprintf(asm_filename, sizeof(asm_filename), "%s.asm", argv[1]);
    asm_file = fopen(asm_filename, "w");
    if (!asm_file) {
        fprintf(stderr, "Cannot create output file %s\n", asm_filename);
        fclose(yyin);
        return 1;
    }

    int result = yyparse();
    
    fclose(yyin);
    fclose(asm_file);

    if (result == 0) {
        printf("Parsing completed successfully.\n");
        printf("Assembly code generated in %s\n", asm_filename);
    }

    return result;
}