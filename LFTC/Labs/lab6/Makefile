CC=gcc
CFLAGS=-Wall

minicompiler: lex.yy.c parser.tab.c
	$(CC) $(CFLAGS) -o minicompiler lex.yy.c parser.tab.c

parser.tab.c parser.tab.h: parser.y
	bison -d parser.y

lex.yy.c: lexer.l parser.tab.h
	flex lexer.l

test: minicompiler test-input
	./minicompiler test-input
	nasm -f elf64 test-input.asm -o test-input.o
	gcc -no-pie test-input.o -o test-input.exe

clean:
	rm -f minicompiler lex.yy.c parser.tab.c parser.tab.h *.o *.asm *.exe

.PHONY: clean test