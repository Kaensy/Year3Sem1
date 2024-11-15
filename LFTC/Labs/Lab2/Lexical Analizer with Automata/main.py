from typing import List, Tuple, Set, Dict


class FiniteAutomaton:
    def __init__(self):
        self.states: Set[str] = set()
        self.alphabet: Set[str] = set()
        self.transitions: Dict[Tuple[str, str], str] = {}
        self.initial_state: str = ''
        self.final_states: Set[str] = set()

    def add_transition(self, from_state: str, symbol: str, to_state: str):
        self.states.add(from_state)
        self.states.add(to_state)
        self.alphabet.add(symbol)
        self.transitions[(from_state, symbol)] = to_state

    def check_sequence(self, sequence: str) -> bool:
        if not sequence:
            return False

        current_state = self.initial_state

        for char in sequence:
            if char not in self.alphabet:
                return False

            transition_key = (current_state, char)
            if transition_key not in self.transitions:
                return False

            current_state = self.transitions[transition_key]

        return current_state in self.final_states


class HashTable:
    def __init__(self, size: int):
        self.size = size
        self.table = [[] for _ in range(size)]
        self.next_index = 1
        self.item_count = 0

    def hash_function(self, key: str) -> int:
        return sum(ord(char) for char in key) % self.size

    def insert(self, key: str) -> int:
        hash_value = self.hash_function(key)

        # Check if key already exists
        for existing_key, index in self.table[hash_value]:
            if existing_key == key:
                return index

        # Add new key with next available index
        self.table[hash_value].append((key, self.next_index))
        self.item_count += 1
        self.next_index += 1

        # Resize if load factor exceeds 0.75
        if self.item_count / self.size > 0.75:
            self._resize()

        return self.next_index - 1

    def _resize(self):
        old_table = self.table
        self.size *= 2
        self.table = [[] for _ in range(self.size)]
        self.item_count = 0

        # Rehash all items
        for bucket in old_table:
            for key, index in bucket:
                hash_value = self.hash_function(key)
                self.table[hash_value].append((key, index))
                self.item_count += 1

    def get_sorted_items(self):
        """Return all items sorted by index"""
        items = []
        for bucket in self.table:
            items.extend(bucket)
        return sorted(items, key=lambda x: x[1])

    def write_to_file(self, filename: str):
        with open(filename, 'w') as f:
            f.write("TS :\n")
            f.write("Symbol | Index | Hash\n")
            f.write("-" * 30 + "\n")

            # Get items sorted by index
            sorted_items = self.get_sorted_items()

            # Write entries with hash value for verification
            for symbol, index in sorted_items:
                hash_value = self.hash_function(symbol)
                f.write(f"{symbol:<15}| {index:<6}| {hash_value}\n")

    def __str__(self):
        """String representation showing hash table structure"""
        result = []
        for i, bucket in enumerate(self.table):
            if bucket:  # Only show non-empty buckets
                result.append(f"Bucket {i}: {bucket}")
        return "\n".join(result)


class LexicalAnalyzer:
    def __init__(self):
        self.identifier_fa = self._create_identifier_automaton()
        self.integer_fa = self._create_integer_automaton()
        self.real_fa = self._create_real_number_automaton()

        # Hardcoded atoms dictionary with predefined codes
        self.atoms_dict = {
            "ID": 0,
            "CONST": 1,
            "int": 2,
            "double": 3,
            "void": 4,
            "main": 5,
            "cout": 6,
            "cin": 7,
            "while": 8,
            "if": 9,
            "else": 10,
            "endl": 11,
            "+": 12,
            "-": 13,
            "*": 14,
            "<<": 15,
            ">>": 16,
            "=": 17,
            "!=": 18,
            ">": 19,
            "<": 20,
            "<=": 21,
            ">=": 22,
            "==": 23,
            "[": 24,
            "]": 25,
            "(": 26,
            ")": 27,
            "{": 28,
            "}": 29,
            ",": 30,
            ";": 31
        }

        # Categorize tokens
        self.keywords = {"int", "double", "void", "main", "cout", "cin", "while", "if", "else", "endl"}
        self.operators = {"+", "-", "*", "<<", ">>", "=", "!=", ">", "<", "<=", ">=", "==", "[", "]"}
        self.separators = {"(", ")", "{", "}", ",", ";"}

        self.symbol_table = HashTable(100)
        self.fip = []  # Format: (token, code, symbol_table_position)
        self.errors = []

        self.current_line = 1
        self.current_column = 1
        self.line_start_positions = [0]

    def _create_identifier_automaton(self) -> FiniteAutomaton:
        fa = FiniteAutomaton()
        fa.states = {'q0', 'q1'}
        fa.initial_state = 'q0'
        fa.final_states = {'q1'}

        # Letters for first character
        letters = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'
        for letter in letters:
            fa.add_transition('q0', letter, 'q1')

        # Letters, digits, and underscore for subsequent characters
        for char in letters + '0123456789_':
            fa.add_transition('q1', char, 'q1')

        return fa

    def _create_integer_automaton(self) -> FiniteAutomaton:
        """
        Based on C++17 Standard (ISO/IEC 14882:2017) Section 5.13.2
        Integer literals can be:
        - Decimal: A non-zero decimal digit (1-9), followed by zero or more decimal digits (0-9)
        - Zero by itself
        - Optional sign (+ or -)
        """
        fa = FiniteAutomaton()

        # States:
        # q0: initial state
        # q1: after sign
        # q2: after zero
        # q3: after first non-zero digit
        fa.states = {'q0', 'q1', 'q2', 'q3'}
        fa.initial_state = 'q0'
        fa.final_states = {'q2', 'q3'}  # accepting states for zero and non-zero numbers

        # Sign transitions
        fa.add_transition('q0', '+', 'q1')
        fa.add_transition('q0', '-', 'q1')

        # Zero transitions
        fa.add_transition('q0', '0', 'q2')  # zero from initial state
        fa.add_transition('q1', '0', 'q2')  # zero after sign

        # Non-zero digit transitions
        for digit in '123456789':
            fa.add_transition('q0', digit, 'q3')  # from initial state
            fa.add_transition('q1', digit, 'q3')  # after sign
            fa.add_transition('q3', digit, 'q3')  # subsequent digits

        # Zero after non-zero digit
        fa.add_transition('q3', '0', 'q3')

        return fa

    def _create_real_number_automaton(self) -> FiniteAutomaton:
        fa = FiniteAutomaton()

        # States:
        # q0: initial state
        # q1: after sign
        # q2: after integer part
        # q3: after decimal point
        # q4: after decimal digits
        fa.states = {'q0', 'q1', 'q2', 'q3', 'q4'}
        fa.initial_state = 'q0'
        fa.final_states = {'q4'}

        # Sign transitions
        fa.add_transition('q0', '+', 'q1')
        fa.add_transition('q0', '-', 'q1')

        # Integer part transitions
        for digit in '0123456789':
            fa.add_transition('q0', digit, 'q2')  # from initial state
            fa.add_transition('q1', digit, 'q2')  # after sign
            fa.add_transition('q2', digit, 'q2')  # more digits

        # Decimal point
        fa.add_transition('q2', '.', 'q3')

        # Decimal part (must have at least one digit)
        for digit in '0123456789':
            fa.add_transition('q3', digit, 'q4')  # first decimal digit
            fa.add_transition('q4', digit, 'q4')  # more decimal digits

        return fa


    def load_atoms(self, filename: str):
        try:
            with open(filename, 'r') as f:
                for line in f:
                    token, code = line.strip().split()
                    code = int(code)
                    self.atoms_dict[token] = code

                    # Categorize tokens
                    if token in {'int', 'double', 'void', 'main', 'cout', 'cin', 'while', 'if', 'else', 'endl'}:
                        self.keywords.add(token)
                    elif token in {'+', '-', '*', '<<', '>>', '=', '!=', '>', '<', '<=', '>=', '==', '[', ']'}:
                        self.operators.add(token)
                    elif token in {'(', ')', '{', '}', ',', ';'}:
                        self.separators.add(token)
        except FileNotFoundError:
            print(f"Error: {filename} not found. Creating default atoms file...")
            self.create_atoms_file()
            self.load_atoms(filename)

    def tokenize(self, content: str) -> List[Tuple[str, int, int]]:
        """Returns list of (token, line_number, column_number)"""
        tokens = []
        current_token = ''
        i = 0
        token_start_line = 1
        token_start_column = 1

        while i < len(content):
            char = content[i]

            # Track position
            if char == '\n':
                self.current_line += 1
                self.current_column = 1
                self.line_start_positions.append(i + 1)
            else:
                self.current_column += 1

            # Handle string constants
            if char == '"':
                if current_token:
                    tokens.append((current_token, token_start_line, token_start_column))
                token_start_line = self.current_line
                token_start_column = self.current_column
                current_token = char
                i += 1

                # Continue until closing quote or end of file
                while i < len(content) and content[i] != '"':
                    current_token += content[i]
                    if content[i] == '\n':
                        self.current_line += 1
                        self.current_column = 1
                        self.line_start_positions.append(i + 1)
                    else:
                        self.current_column += 1
                    i += 1

                if i < len(content):  # Found closing quote
                    current_token += content[i]
                    tokens.append((current_token, token_start_line, token_start_column))
                    current_token = ''
                else:  # EOF before closing quote
                    self.errors.append(
                        f"Error at line {token_start_line}, column {token_start_column}: Unclosed string constant")
                i += 1
                continue

            # Handle operators and separators
            if char in self.operators or char in self.separators:
                # Check for compound operators
                if i + 1 < len(content):
                    compound = char + content[i + 1]
                    if compound in self.operators:
                        if current_token:
                            tokens.append((current_token, token_start_line, token_start_column))
                        tokens.append((compound, self.current_line, self.current_column))
                        current_token = ''
                        i += 2
                        self.current_column += 1
                        continue

                # Single character operator/separator
                if current_token:
                    tokens.append((current_token, token_start_line, token_start_column))
                tokens.append((char, self.current_line, self.current_column))
                current_token = ''
                i += 1
                continue

            # Handle whitespace
            if char.isspace():
                if current_token:
                    tokens.append((current_token, token_start_line, token_start_column))
                    current_token = ''
                i += 1
                continue

            # Start new token
            if not current_token:
                token_start_line = self.current_line
                token_start_column = self.current_column

            # Build token
            current_token += char
            i += 1

        # Add final token
        if current_token:
            tokens.append((current_token, token_start_line, token_start_column))

        return tokens

    def classify_token(self, token: str) -> Tuple[str, int, str]:
        # Remove regex-based checks and use finite automata

        # Check keywords (exact match)
        if token in self.keywords:
            return token, self.atoms_dict[token], "Keyword"

        # Check operators and separators (exact match)
        if token in self.operators:
            return token, self.atoms_dict[token], "Operator"
        if token in self.separators:
            return token, self.atoms_dict[token], "Delimiter"

        # Check string constants
        if len(token) >= 2 and token[0] == '"' and token[-1] == '"':
            return "CONST", self.atoms_dict["CONST"], "CONST"

        # Use finite automata for identification
        if self.identifier_fa.check_sequence(token):
            return "ID", self.atoms_dict["ID"], "ID"

        if self.integer_fa.check_sequence(token):
            return "CONST", self.atoms_dict["CONST"], "CONST"

        if self.real_fa.check_sequence(token):
            return "CONST", self.atoms_dict["CONST"], "CONST"

        return 'unknown', -1, "Unknown"

    def analyze(self, filename: str):
        try:
            with open(filename, 'r') as file:
                content = file.read()
        except FileNotFoundError:
            print(f"Error: File {filename} not found")
            return

        # Reset state
        self.fip = []
        self.errors = []
        self.current_line = 1
        self.current_column = 1
        self.line_start_positions = [0]

        # Get tokens with positions
        tokens_with_pos = self.tokenize(content)

        # Process each token
        for token, line, column in tokens_with_pos:
            token_type, code, type_name = self.classify_token(token)

            if token_type == 'unknown':
                self.errors.append(f"Error at line {line}, column {column}: Invalid token '{token}'")
                continue

            # Add to symbol table if identifier or constant
            if token_type in {"ID", "CONST"}:
                symbol_table_position = self.symbol_table.insert(token)
            else:
                symbol_table_position = "-"

            self.fip.append((token, code, symbol_table_position, type_name, line, column))

    def write_results(self):
        # Write FIP to file
        with open('fip_output.txt', 'w') as f:
            f.write("FIP :\n")
            f.write("Token | Atom ID | TS Position | Token Type | Line | Column\n")
            f.write("-" * 70 + "\n")
            for token, code, st_pos, token_type, line, col in self.fip:
                f.write(f"{token:<15}| {code:<8}| {str(st_pos):<12}| {token_type:<10}| {line:<5}| {col}\n")

        # Write Symbol Table
        self.symbol_table.write_to_file('ts_output.txt')

        # Print errors with locations
        if self.errors:
            print("\nLexical Errors Found:")
            for error in self.errors:
                print(error)
        else:
            print("\nNo lexical errors found.")

        # Additional error statistics
        if self.errors:
            print(f"\nTotal number of lexical errors: {len(self.errors)}")
            # Group errors by line
            errors_by_line = {}
            for error in self.errors:
                line_num = int(error.split('line')[1].split(',')[0])
                errors_by_line.setdefault(line_num, []).append(error)

            print("\nErrors by line:")
            for line_num in sorted(errors_by_line.keys()):
                print(f"\nLine {line_num} ({len(errors_by_line[line_num])} errors):")
                for error in errors_by_line[line_num]:
                    print(f"  {error}")


def main():
    analyzer = LexicalAnalyzer()
    analyzer.load_atoms('atoms_id.txt')  # Load the created atoms file
    analyzer.analyze('input_program.txt')
    # analyzer.analyze('input_program_errors.txt')
    analyzer.write_results()


if __name__ == "__main__":
    main()