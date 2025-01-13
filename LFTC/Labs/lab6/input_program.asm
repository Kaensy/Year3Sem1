section .data
    fmt_input db "%lld", 0
    fmt_output db "%lld", 10, 0
section .bss
    x resq 1
    y resq 1
    result resq 1
section .text
    global main
    extern printf
    extern scanf
main:
    push rbp
    mov rbp, rsp
    sub rsp, 32
    lea rsi, [x]
    lea rdi, [fmt_input]
    xor eax, eax
    call scanf
    lea rsi, [y]
    lea rdi, [fmt_input]
    xor eax, eax
    call scanf
    mov rax, [x]
    push rax
    mov rax, [y]
    push rax
    mov rbx, rax
    pop rax
    imul rax, rbx
    mov rax, 2
    push rax
    push rax
    mov rbx, rax
    pop rax
    add rax, rbx
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
