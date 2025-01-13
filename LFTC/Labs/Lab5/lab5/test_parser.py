from ll1_parser import Grammar


def print_table(parsing_table):
    """Print parsing table in a formatted way"""
    # Find all terminals
    terminals = set()
    non_terminals = set()
    for (nt, t) in parsing_table.keys():
        terminals.add(t)
        non_terminals.add(nt)

    # Print header
    terminals = sorted(terminals)
    print("\nParsing Table:")
    print(f"{'NT\\T':<10}", end='')
    for t in terminals:
        print(f"{t:<15}", end='')
    print()
    print("-" * (10 + 15 * len(terminals)))

    # Print rows
    for nt in sorted(non_terminals):
        print(f"{nt:<10}", end='')
        for t in terminals:
            if (nt, t) in parsing_table:
                prod = ' '.join(parsing_table[(nt, t)])
                print(f"{prod:<15}", end='')
            else:
                print(f"{'empty':<15}", end='')
        print()


def test_parser():
    grammar = Grammar()

    try:
        grammar.read_from_file('grammar.txt')
        print("Grammar loaded successfully!")

        # Print grammar details
        print("\nProductions:")
        for nt, prods in grammar.productions.items():
            for prod in prods:
                print(f"{nt:<5} -> {' '.join(prod)}")

        # Compute and display FIRST/FOLLOW sets
        first_sets = grammar.compute_first_sets()
        follow_sets = grammar.compute_follow_sets(first_sets)

        print("\nFIRST sets:")
        for symbol in sorted(first_sets.keys()):
            print(f"FIRST({symbol:<5}) = {first_sets[symbol]}")

        print("\nFOLLOW sets:")
        for nt in sorted(grammar.non_terminals):
            print(f"FOLLOW({nt:<5}) = {follow_sets[nt]}")

        # Build and display parsing table
        parsing_table = grammar.build_parsing_table()
        print_table(parsing_table)

        # Test cases
        test_inputs = [
            "id + id * id",
            "( id + id ) * id",
            "id * id",
            "id",  # Simple case
            "( id )",  # Parentheses
            "id + id"  # Addition
        ]

        print("\nTesting parser with input strings:")
        for input_string in test_inputs:
            print(f"\nInput: {input_string}")
            try:
                derivation = grammar.parse(input_string)
                print("✓ Valid! Derivation steps:")
                for i, step in enumerate(derivation, 1):
                    print(f"{i:2d}. {' '.join(step)}")
            except Exception as e:
                print(f"✗ Invalid: {str(e)}")

    except Exception as e:
        print(f"Error during parsing: {str(e)}")
        raise


if __name__ == "__main__":
    test_parser()