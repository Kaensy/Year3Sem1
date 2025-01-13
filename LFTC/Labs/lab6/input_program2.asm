section .data
    fmt_input db "%lld", 0
    fmt_output db "%lld", 10, 0
section .bss
    a resq 1
    b resq 1
    c resq 1
    result resq 1
section .text
    global main
    extern printf
    extern scanf
main:
    push rbp
    mov rbp, rsp
    sub rsp, 32
    lea rsi, [a]
    lea rdi, [fmt_input]
    xor eax, eax
    call scanf
    lea rsi, [b]
    lea rdi, [fmt_input]
    xor eax, eax
    call scanf
    lea rsi, [c]
    lea rdi, [fmt_input]
    xor eax, eax
    call scanf
    mov rax, [a]
    push rax
    mov rax, [b]
    push rax
    push rax
    mov rbx, rax
    pop rax
    add rax, rbx
    mov rax, [c]
    push rax
    mov rax, 2
    push rax
    push rax
    mov rbx, rax
    pop rax
    add rax, rbx
    mov rbx, rax
    pop rax
    imul rax, rbx
    mov [result], rax
    mov rax, [result]
    push rax
    mov rsi, rax
    lea rdi, [fmt_output]
    xor eax, eax
    call printf
    xor eax, eax
    leave
    ret
