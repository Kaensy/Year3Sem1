import re


class HashNode:
    def __init__(self, key, value):
        self.key = key
        self.value = value
        self.next = None


class HashTable:
    def __init__(self, size):
        self.size = size
        self.table = [None] * size

    def hash_function(self, key):
        return sum(ord(char) for char in key) % self.size

    def insert(self, key, value):
        index = self.hash_function(key)
        if not self.table[index]:
            self.table[index] = HashNode(key, value)
        else:
            current = self.table[index]
            while current.next:
                if current.key == key:
                    current.value = value
                    return
                current = current.next
            if current.key == key:
                current.value = value
            else:
                current.next = HashNode(key, value)

    def get(self, key):
        index = self.hash_function(key)
        current = self.table[index]
        while current:
            if current.key == key:
                return current.value
            current = current.next
        return None

    def __iter__(self):
        for bucket in self.table:
            current = bucket
            while current:
                yield (current.key, current.value)
                current = current.next


class LexicalAnalyzer:
    def __init__(self, atoms_id_file):
        self.atoms_id = self.load_atoms_id(atoms_id_file)
        self.keywords = ["int", "double", "void", "main", "cout", "cin", "while", "if", "else", "endl"]
        self.operators = ["+", "-", "*", "<<", ">>", "=", "!=", ">", "<", "<=", ">=", "==", "[", "]"]
        self.delimiters = ["(", ")", "{", "}", ",", ";"]
        self.fip = []
        self.ts = HashTable(101)
        self.ts_counter = 1
        self.errors = []
        self.in_declaration_section = True
        self.current_line_tokens = []
        self.line_number = 0
        self.main_opened = False
        self.main_closed = False

    def load_atoms_id(self, file_path):
        atoms_id = {}
        try:
            with open(file_path, 'r') as file:
                for line_number, line in enumerate(file, 1):
                    line = line.strip()
                    if not line or line.startswith('#'):  # Skip empty lines and comments
                        continue
                    parts = line.split()
                    if len(parts) != 2:
                        print(f"Warning: Invalid format in line {line_number}: {line}")
                        continue
                    token, id_str = parts
                    try:
                        atoms_id[token] = int(id_str)
                    except ValueError:
                        print(f"Warning: Invalid ID in line {line_number}: {line}")
        except FileNotFoundError:
            print(f"Error: The file '{file_path}' was not found.")
            exit(1)
        except Exception as e:
            print(f"An error occurred while reading the file: {e}")
            exit(1)
        return atoms_id

    def is_identifier(self, token):
        return re.match(r'^[a-zA-Z][a-zA-Z0-9_]*$', token) is not None

    def is_constant(self, token):
        return (re.match(r'^".*"$', token) is not None or  # string constant
                re.match(r'^[+-]?(\d*\.)?\d+$', token) is not None)  # number constant

    def get_token_type(self, token):
        if token in self.keywords:
            return "Keyword"
        elif token in self.operators:
            return "Operator"
        elif token in self.delimiters:
            return "Delimiter"
        elif self.is_identifier(token):
            return "ID"
        elif self.is_constant(token):
            return "CONST"
        else:
            return "Unknown"

    def add_to_ts(self, token):
        existing_value = self.ts.get(token)
        if existing_value is None:
            self.ts.insert(token, self.ts_counter)
            self.ts_counter += 1
            return self.ts_counter - 1
        return existing_value

    def check_instruction_format(self):
        if not self.current_line_tokens:
            return

        if self.line_number == 1:
            if self.current_line_tokens != ['int', 'main', '(', ')']:
                self.errors.append(f"Error on line {self.line_number}: Invalid program start. Expected 'int main()'")
            return

        if self.line_number == 2:
            if self.current_line_tokens != ['{']:
                self.errors.append(f"Error on line {self.line_number}: Expected opening brace '{{' for main function")
            else:
                self.main_opened = True
            return

        if self.current_line_tokens == ['}']:
            if self.main_opened and not self.main_closed:
                self.main_closed = True
            else:
                self.errors.append(
                    f"Error on line {self.line_number}: Unexpected closing brace '}}' or multiple closing braces for main function")
            return

        if not self.main_opened:
            self.errors.append(f"Error on line {self.line_number}: Code outside of main function")
            return

        if self.main_closed:
            self.errors.append(f"Error on line {self.line_number}: Code after main function closed")
            return

        if self.in_declaration_section:
            if self.current_line_tokens[0] in ['int', 'double']:
                self.check_declaration()
            else:
                self.in_declaration_section = False
                self.check_instruction()
        else:
            if self.current_line_tokens[0] in ['int', 'double']:
                self.errors.append(
                    f"Error on line {self.line_number}: Declaration not allowed here. All declarations must be at the beginning.")
            else:
                self.check_instruction()

    def check_declaration(self):
        if not self.current_line_tokens[-1] == ';':
            self.errors.append(f"Error on line {self.line_number}: Missing semicolon in declaration")

        # Check for valid declaration format
        if len(self.current_line_tokens) < 3:
            self.errors.append(f"Error on line {self.line_number}: Invalid declaration format")
            return

        type_token = self.current_line_tokens[0]
        identifiers = self.current_line_tokens[1:-1]  # Exclude type and semicolon

        for i, token in enumerate(identifiers):
            if i % 2 == 0:  # Should be an identifier
                if not self.is_identifier(token):
                    self.errors.append(f"Error on line {self.line_number}: Invalid identifier '{token}' in declaration")
            else:  # Should be a comma
                if token != ',':
                    self.errors.append(
                        f"Error on line {self.line_number}: Expected ',' between identifiers in declaration")

    def check_instruction(self):
        if self.current_line_tokens[0] in ['if', 'while']:
            self.check_conditional()
        elif self.current_line_tokens[0] in ['cin', 'cout']:
            self.check_io()
        elif '=' in self.current_line_tokens:
            self.check_assignment()
        else:
            self.errors.append(f"Error on line {self.line_number}: Invalid instruction")

    def check_conditional(self):
        if self.current_line_tokens[0] not in ['if',
                                               'while'] or '(' not in self.current_line_tokens or ')' not in self.current_line_tokens:
            self.errors.append(f"Error on line {self.line_number}: Invalid conditional statement format")

    def tokenize_line(self, line):
        return re.findall(r'<<|>>|\b\d*\.\d+\b|\b\w+\b|"[^"]*"|\S', line)

    def check_io(self):
        if self.current_line_tokens[0] == 'cin':
            if '>>' not in self.current_line_tokens or self.current_line_tokens[-1] != ';':
                self.errors.append(f"Error on line {self.line_number}: Invalid input statement format")
        elif self.current_line_tokens[0] == 'cout':
            if '<<' not in self.current_line_tokens or self.current_line_tokens[-1] != ';':
                self.errors.append(f"Error on line {self.line_number}: Invalid output statement format")

    def check_assignment(self):
        if '=' not in self.current_line_tokens or self.current_line_tokens[-1] != ';':
            self.errors.append(f"Error on line {self.line_number}: Invalid assignment format")

    def analyze(self, input_file):
        try:
            with open(input_file, 'r') as file:
                content = file.readlines()
        except FileNotFoundError:
            print(f"Error: The input file '{input_file}' was not found.")
            exit(1)
        except Exception as e:
            print(f"An error occurred while reading the input file: {e}")
            exit(1)

        for self.line_number, line in enumerate(content, 1):
            self.current_line_tokens = self.tokenize_line(line.strip())
            self.check_instruction_format()

            for token in self.current_line_tokens:
                token_type = self.get_token_type(token)

                if token_type == "Unknown":
                    self.errors.append(f"Error on line {self.line_number}: Invalid token '{token}'")
                    continue

                atom_id = self.atoms_id.get(token, self.atoms_id.get(token_type, -1))

                if token_type in ["ID", "CONST"]:
                    ts_position = self.add_to_ts(token)
                else:
                    ts_position = "-"

                self.fip.append((token, atom_id, ts_position, token_type))

        if not self.main_opened:
            self.errors.append("Error: Missing opening brace '{' for main function")
        if not self.main_closed:
            self.errors.append("Error: Missing closing brace '}' for main function")

    def report_errors(self):
        if self.errors:
            print("Lexical and syntactic errors found:")
            for error in self.errors:
                print(error)
        else:
            print("No errors found.")

    def write_output(self, fip_file, ts_file):
        with open(fip_file, 'w') as f:
            f.write("FIP :\n")
            f.write("Token | Atom ID | TS Position | Token Type\n")
            f.write("-" * 50 + "\n")
            for token, atom_id, ts_pos, token_type in self.fip:
                f.write(f"{token:<15}| {atom_id:<8}| {ts_pos:<12}| {token_type}\n")

        with open(ts_file, 'w') as f:
            f.write("TS :\n")
            f.write("Symbol | Index\n")
            f.write("-" * 20 + "\n")
            for symbol, index in self.ts:
                f.write(f"{symbol:<15}| {index}\n")


# Usage
analyzer = LexicalAnalyzer('atoms_id.txt')
analyzer.analyze('input_program.txt')
analyzer.report_errors()
analyzer.write_output('fip_output.txt', 'ts_output.txt')
