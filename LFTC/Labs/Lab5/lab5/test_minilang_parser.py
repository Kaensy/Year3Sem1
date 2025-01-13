from minilang_parser import MinilangParser, FIPEntry
from typing import List
import sys


def create_test_fip(parser: MinilangParser, program: str) -> List[FIPEntry]:
    """Creates a FIP for testing purposes from a program string"""
    tokens = []
    lines = program.strip().split('\n')

    # Simple tokenizer for testing
    for line_num, line in enumerate(lines, 1):
        line = line.strip()
        if not line or line.startswith('#'):
            continue

        # Split line into tokens
        current_token = ''
        i = 0
        while i < len(line):
            # Handle special cases (operators, etc.)
            if line[i:i + 2] in ['<<', '>>', '<=', '>=', '==', '!=']:
                if current_token:
                    tokens.append(current_token)
                    current_token = ''
                tokens.append(line[i:i + 2])
                i += 2
                continue

            if line[i] in '+-*=(){},;[]<>':
                if current_token:
                    tokens.append(current_token)
                    current_token = ''
                tokens.append(line[i])
                i += 1
                continue

            if line[i].isspace():
                if current_token:
                    tokens.append(current_token)
                    current_token = ''
                i += 1
                continue

            current_token += line[i]
            i += 1

        if current_token:
            tokens.append(current_token)

    # Convert tokens to FIP entries
    fip_entries = []
    symbol_table_counter = 1
    symbol_table = {}

    for token in tokens:
        if token in parser.token_codes:
            # Known token
            fip_entries.append(FIPEntry(token, parser.token_codes[token], -1))
        elif token.isidentifier():
            # Identifier
            if token not in symbol_table:
                symbol_table[token] = symbol_table_counter
                symbol_table_counter += 1
            fip_entries.append(FIPEntry(token, 0, symbol_table[token]))  # 0 for ID
        elif token.replace('.', '', 1).isdigit():
            # Numeric constant
            if token not in symbol_table:
                symbol_table[token] = symbol_table_counter
                symbol_table_counter += 1
            fip_entries.append(FIPEntry(token, 1, symbol_table[token]))  # 1 for CONST
        else:
            print(f"Warning: Unknown token '{token}'")

    return fip_entries


def test_parser():
    parser = MinilangParser()

    try:
        # Read grammar
        print("Reading grammar...")
        parser.read_grammar("minilang_grammar.txt")

        # Test each program
        test_programs = [
            ("Circle area and perimeter", """
                int main() {
                    double radius, area, perimeter;
                    cin >> radius;
                    area = radius * radius * 3.14;
                    perimeter = 2 * 3.14 * radius;
                    cout << area;
                    cout << perimeter;
                }
            """),
            ("GCD", """
                int main() {
                    int a, b, temp;
                    cin >> a;
                    cin >> b;
                    while (b != 0) {
                        temp = b;
                        b = a - (a / b) * b;
                        a = temp;
                    }
                    cout << a;
                }
            """),
            ("Sum of N numbers", """
                int main() {
                    int n, sum, i, num;
                    sum = 0;
                    cin >> n;
                    i = 0;
                    while (i < n) {
                        cin >> num;
                        sum = sum + num;
                        i = i + 1;
                    }
                    cout << sum;
                }
            """)
        ]

        # Test each program
        for program_name, program_code in test_programs:
            print(f"\nTesting program: {program_name}")
            print("=" * 50)

            try:
                # Create FIP entries
                fip_entries = create_test_fip(parser, program_code)

                print(f"Generated {len(fip_entries)} FIP entries")
                print("\nFIP entries preview:")
                for i, entry in enumerate(fip_entries[:10]):
                    print(
                        f"{i + 1:2d}. Token: {entry.token:15} Code: {entry.code:2d} ST_Pos: {entry.symbol_table_pos:2d}")
                if len(fip_entries) > 10:
                    print("...")

                # Parse program
                print("\nParsing program...")
                try:
                    derivation = parser.parse_fip(fip_entries)
                    print("✓ Program parsed successfully!")
                    print("\nFirst 10 derivation steps:")
                    for i, step in enumerate(derivation[:10], 1):
                        print(f"{i:2d}. {' '.join(step)}")
                    if len(derivation) > 10:
                        print(f"... ({len(derivation)} steps total)")
                except Exception as e:
                    if "not LL(1)" in str(e):
                        print(f"✗ Grammar Error: {str(e)}")
                        print("This means we need to modify our grammar to eliminate ambiguity.")
                    else:
                        print(f"✗ Parsing Error: {str(e)}")
                        print("Current token sequence at error:")
                        for i, entry in enumerate(fip_entries[:10]):
                            print(f"{i + 1:2d}. {entry.token}")

            except Exception as e:
                print(f"✗ Error in FIP generation: {str(e)}")

            print("\n" + "=" * 50)

    except Exception as e:
        print(f"Error in test setup: {str(e)}")
        raise


if __name__ == "__main__":
    test_parser()