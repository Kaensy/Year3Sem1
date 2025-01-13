%{
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include "parser.tab.h"

extern int yylineno;
extern FILE* yyin;
void yyerror(const char* s);

// Buffer for string literals
char string_buffer[1024];
char* string_buf_ptr;
%}

%option noyywrap
%option yylineno
%option nounput
%option noinput

/* Improved definitions */
DIGIT           [0-9]
DIGIT_POS       [1-9]
LETTER          [a-zA-Z]
WHITESPACE      [ \t\r]
NEWLINE         \n
ID              {LETTER}({LETTER}|{DIGIT})*
NUMBER          {DIGIT_POS}{DIGIT}*|"0"
INVALID_ID      {DIGIT}+{LETTER}+

%%

{WHITESPACE}+   { /* Skip whitespace */ }
{NEWLINE}+      { /* Count lines */ }

"program"       { return PROGRAM; }
"var"          { return VAR; }
"end"          { return END; }
"read"         { return READ; }
"write"        { return WRITE; }

":="           { return ASSIGN; }
"+"            { return PLUS; }
"-"            { return MINUS; }
"*"            { return MULT; }
"("            { return LPAREN; }
")"            { return RPAREN; }
";"            { return SEMICOLON; }
","            { return COMMA; }

{ID}           {
                   if (yyleng > 255) {
                       yyerror("Identifier too long");
                   } else {
                       yylval.strval = strdup(yytext);
                       return IDENTIFIER;
                   }
               }

{NUMBER}       {
                   long val = strtol(yytext, NULL, 10);
                   if (val > INT_MAX) {
                       yyerror("Integer constant too large");
                   } else {
                       yylval.num = (int)val;
                       return NUMBER;
                   }
               }

{INVALID_ID}   { 
                   char msg[100];
                   snprintf(msg, sizeof(msg), "Invalid identifier: %s", yytext);
                   yyerror(msg);
               }

.              { 
                   char msg[100];
                   snprintf(msg, sizeof(msg), "Unexpected character: %s", yytext);
                   yyerror(msg);
               }

%%