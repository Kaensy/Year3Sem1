class FiniteAutomaton:
    def __init__(self):
        self.alphabet = set()
        self.states = set()
        self.initial_state = None
        self.final_states = set()
        self.transitions = {}  # Format: {(current_state, symbol): set(next_states)}

    def read_from_keyboard(self):
        # Read alphabet first
        alphabet_input = input("Enter alphabet symbols (comma-separated): ").strip()
        self.alphabet = {s.strip() for s in alphabet_input.split(',')}

        # Read states
        states_input = input("Enter states (comma-separated): ").strip()
        self.states = {s.strip() for s in states_input.split(',')}

        # Read initial state
        self.initial_state = input("Enter initial state: ").strip()

        # Read final states
        final_states_input = input("Enter final states (comma-separated): ").strip()
        self.final_states = {s.strip() for s in final_states_input.split(',')}

        # Read transitions
        print("Enter transitions (format: 'state1,symbol,state2', empty line to finish):")
        while True:
            transition = input().strip()
            if not transition:
                break
            state1, symbol, state2 = [x.strip() for x in transition.split(',')]
            if symbol not in self.alphabet:
                print(f"Warning: Symbol '{symbol}' not in alphabet")
                continue
            if state1 not in self.states or state2 not in self.states:
                print(f"Warning: State(s) not in defined states set")
                continue
            self.transitions.setdefault((state1, symbol), set()).add(state2)

    def read_from_file(self, filename):
        """Read FA elements from a file"""
        with open(filename, 'r') as file:
            # Read entire file content
            content = file.read()

            # Parse alphabet
            alphabet_start = content.find("Alphabet:") + 9
            alphabet_end = content.find("\n", alphabet_start)
            alphabet_str = content[alphabet_start:alphabet_end]
            alphabet_str = alphabet_str[alphabet_str.find("{") + 1:alphabet_str.find("}")]
            self.alphabet = {s.strip() for s in alphabet_str.split(',')}

            # Parse states
            states_start = content.find("States:") + 7
            states_end = content.find("\n", states_start)
            states_str = content[states_start:states_end]
            states_str = states_str[states_str.find("{") + 1:states_str.find("}")]
            self.states = {s.strip() for s in states_str.split(',')}

            # Parse initial state
            initial_start = content.find("Initial:") + 8
            initial_end = content.find("\n", initial_start)
            self.initial_state = content[initial_start:initial_end].strip()

            # Parse final states
            final_start = content.find("Final:") + 6
            final_end = content.find("\n", final_start)
            final_str = content[final_start:final_end]
            final_str = final_str[final_str.find("{") + 1:final_str.find("}")]
            self.final_states = {s.strip() for s in final_str.split(',')}

            # Parse transitions
            transitions_start = content.find("Transitions:") + 12
            transitions_str = content[transitions_start:]
            transitions_str = transitions_str[transitions_str.find("{") + 1:transitions_str.find("}")]

            # Clear existing transitions
            self.transitions = {}

            # Process each transition
            for transition in transitions_str.split(';'):
                transition = transition.strip()
                if transition:
                    try:
                        # Parse (q0,a)->q1 format
                        source_symbol, target = transition.split('->')
                        source_symbol = source_symbol.strip()[1:-1]  # Remove parentheses
                        state1, symbol = [x.strip() for x in source_symbol.split(',')]
                        state2 = target.strip()
                        self.transitions.setdefault((state1, symbol), set()).add(state2)
                    except Exception as e:
                        print(f"Error parsing transition: {transition}")
                        print(f"Error details: {e}")

    def validate(self):
        """Validate the automaton's structure"""
        # Check if alphabet is not empty
        if not self.alphabet:
            return False, "Alphabet cannot be empty"

        # Check if states set is not empty
        if not self.states:
            return False, "States set cannot be empty"

        # Check if initial state is in states set
        if self.initial_state not in self.states:
            return False, "Initial state not in states set"

        # Check if all final states are in states set
        if not self.final_states.issubset(self.states):
            return False, "Some final states are not in states set"

        # Check transitions
        for (state, symbol), next_states in self.transitions.items():
            # Check if source state is valid
            if state not in self.states:
                return False, f"Invalid source state in transition: {state}"
            # Check if symbol is in alphabet
            if symbol not in self.alphabet:
                return False, f"Invalid symbol in transition: {symbol}"
            # Check if all target states are valid
            if not next_states.issubset(self.states):
                return False, f"Invalid target state in transition from {state} with {symbol}"

        return True, "Automaton is valid"

    def __str__(self):
        """String representation of the automaton"""
        fa_str = []
        fa_str.append(f"Alphabet: {{{', '.join(sorted(self.alphabet))}}}")
        fa_str.append(f"States: {{{', '.join(sorted(self.states))}}}")
        fa_str.append(f"Initial: {self.initial_state}")
        fa_str.append(f"Final: {{{', '.join(sorted(self.final_states))}}}")

        # Format transitions
        trans_str = []
        for (state, symbol), next_states in sorted(self.transitions.items()):
            for next_state in sorted(next_states):
                trans_str.append(f"({state},{symbol})->{next_state}")
        fa_str.append(f"Transitions: {{{'; '.join(trans_str)}}}")

        return '\n'.join(fa_str)

    def display_alphabet(self):
        """Display the alphabet of the automaton"""
        print("\nAlphabet Σ:")
        if not self.alphabet:
            print("Empty alphabet")
        else:
            print("{" + ", ".join(sorted(self.alphabet)) + "}")

    def display_states(self):
        """Display the set of states"""
        print("\nStates Q:")
        if not self.states:
            print("Empty set of states")
        else:
            print("{" + ", ".join(sorted(self.states)) + "}")
            print(f"Initial state: {self.initial_state}")

    def display_final_states(self):
        """Display the set of final states"""
        print("\nFinal States F:")
        if not self.final_states:
            print("Empty set of final states")
        else:
            print("{" + ", ".join(sorted(self.final_states)) + "}")

    def display_transitions(self):
        """Display the transition function"""
        print("\nTransitions δ:")
        if not self.transitions:
            print("No transitions defined")
        else:
            # Group transitions by source state for better readability
            transitions_by_state = {}
            for (state, symbol), next_states in sorted(self.transitions.items()):
                if state not in transitions_by_state:
                    transitions_by_state[state] = []
                for next_state in sorted(next_states):
                    transitions_by_state[state].append(f"δ({state}, {symbol}) = {next_state}")

            # Print grouped transitions
            for state in sorted(transitions_by_state.keys()):
                print(f"\nFrom state {state}:")
                for transition in transitions_by_state[state]:
                    print(f"  {transition}")

    def is_deterministic(self):
        """
        Check if the automaton is deterministic.
        A DFA should have exactly one transition for each state-symbol pair.
        """
        # Check if initial state exists
        if not self.initial_state:
            return False, "No initial state defined"

        # Check transitions
        for state in self.states:
            for symbol in self.alphabet:
                next_states = self.transitions.get((state, symbol), set())

                # In a DFA, each state-symbol pair should have exactly one next state
                if len(next_states) != 1:
                    if len(next_states) == 0:
                        return False, f"Missing transition for state '{state}' and symbol '{symbol}'"
                    else:
                        return False, f"Multiple transitions found for state '{state}' and symbol '{symbol}'"

        return True, "Automaton is deterministic"

    def get_next_state(self, current_state, symbol):
        """Get the next state for a given current state and symbol in a DFA"""
        next_states = self.transitions.get((current_state, symbol), set())
        return next(iter(next_states)) if next_states else None

    def check_sequence(self, sequence):
        """
        Check if a given sequence is accepted by the DFA.
        Returns: (accepted, trace)
        - accepted: boolean indicating if sequence is accepted
        - trace: list of (state, symbol, next_state) showing the path taken
        """
        # First verify if automaton is deterministic
        is_dfa, message = self.is_deterministic()
        if not is_dfa:
            return False, [], f"Cannot check sequence: {message}"

        # Verify if all symbols in sequence are in the alphabet
        invalid_symbols = [symbol for symbol in sequence if symbol not in self.alphabet]
        if invalid_symbols:
            return False, [], f"Sequence contains invalid symbols: {invalid_symbols}"

        current_state = self.initial_state
        trace = []

        # Process each symbol in the sequence
        for symbol in sequence:
            next_state = self.get_next_state(current_state, symbol)
            if next_state is None:
                return False, trace, f"No transition defined for state '{current_state}' and symbol '{symbol}'"

            trace.append((current_state, symbol, next_state))
            current_state = next_state

        # Check if final state is accepting
        is_accepted = current_state in self.final_states
        return is_accepted, trace, "Sequence accepted" if is_accepted else "Sequence not accepted (not in final state)"

    def display_sequence_check_result(self, sequence, result):
        """Display the result of sequence checking in a formatted way"""
        is_accepted, trace, message = result

        print("\n=== Sequence Check Result ===")
        print(f"Sequence: {sequence}")
        print(f"Status: {'Accepted' if is_accepted else 'Rejected'}")
        print(f"Message: {message}")

        if trace:
            print("\nTransition trace:")
            for current_state, symbol, next_state in trace:
                print(f"  {current_state} --({symbol})--> {next_state}")
            print(
                f"Final state: {trace[-1][2]} ({'accepting' if trace[-1][2] in self.final_states else 'not accepting'})")

    def find_longest_accepted_prefix(self, sequence):
        """
        Find the longest prefix of the given sequence that is accepted by the DFA.
        Returns: (prefix, trace, message)
        - prefix: the longest accepted prefix (empty string if none found)
        - trace: list of (state, symbol, next_state) showing the path
        - message: description of the result
        """
        # First verify if automaton is deterministic
        is_dfa, message = self.is_deterministic()
        if not is_dfa:
            return "", [], f"Cannot check sequence: {message}"

        # Verify if all symbols in sequence are in the alphabet
        # invalid_symbols = [symbol for symbol in sequence if symbol not in self.alphabet]
        # if invalid_symbols:
        #     return "", [], f"Sequence contains invalid symbols: {invalid_symbols}"

        current_state = self.initial_state
        trace = []
        longest_prefix = ""
        longest_prefix_trace = []

        # Process each symbol in the sequence
        for i, symbol in enumerate(sequence):
            next_state = self.get_next_state(current_state, symbol)
            if next_state is None:
                break

            trace.append((current_state, symbol, next_state))
            current_state = next_state

            # If we reach an accepting state, update the longest accepted prefix
            if current_state in self.final_states:
                longest_prefix = sequence[:i + 1]
                longest_prefix_trace = trace.copy()

        # Check if we found any prefix
        if not longest_prefix and self.initial_state in self.final_states:
            return "", [], "Empty string is the longest accepted prefix"
        elif not longest_prefix:
            return "", trace, "No prefix of the sequence is accepted"
        else:
            return longest_prefix, longest_prefix_trace, f"Found longest accepted prefix of length {len(longest_prefix)}"

    def display_prefix_result(self, sequence, result):
        """Display the result of longest prefix finding in a formatted way"""
        prefix, trace, message = result

        print("\n=== Longest Accepted Prefix Result ===")
        print(f"Original sequence: {sequence}")
        print(f"Longest accepted prefix: {prefix if prefix else '(empty string)'}")
        print(f"Prefix length: {len(prefix)}")
        print(f"Message: {message}")

        if trace:
            print("\nTransition trace for longest accepted prefix:")
            for current_state, symbol, next_state in trace:
                print(f"  {current_state} --({symbol})--> {next_state}")
            if trace:
                print(
                    f"Final state: {trace[-1][2]} ({'accepting' if trace[-1][2] in self.final_states else 'not accepting'})")


def display_menu():
    """Display the main menu options"""
    print("\n=== Finite Automaton Menu ===")
    print("1. Display Alphabet")
    print("2. Display States")
    print("3. Display Final States")
    print("4. Display Transitions")
    print("5. Display All")
    print("6. Load Automaton from File")
    print("7. Load Automaton from Keyboard")
    print("8. Validate Automaton")
    print("9. Check if Deterministic")
    print("10. Check Sequence")
    print("11. Find Longest Accepted Prefix")
    print("0. Exit")
    print("==========================")

def main():
    fa = FiniteAutomaton()
    while True:
        display_menu()
        choice = input("\nEnter your choice (0-11): ").strip()

        if choice == '0':
            print("Exiting program...")
            break

        elif choice == '1':
            fa.display_alphabet()

        elif choice == '2':
            fa.display_states()

        elif choice == '3':
            fa.display_final_states()

        elif choice == '4':
            fa.display_transitions()

        elif choice == '5':
            print("\n=== Complete Automaton Definition ===")
            fa.display_alphabet()
            fa.display_states()
            fa.display_final_states()
            fa.display_transitions()

        elif choice == '6':
            filename = input("Enter the filename: ").strip()
            try:
                fa.read_from_file(filename)
                print("Automaton loaded successfully!")
                is_valid, message = fa.validate()
                print(f"Validation: {message}")
            except Exception as e:
                print(f"Error loading file: {e}")

        elif choice == '7':
            try:
                fa.read_from_keyboard()
                print("Automaton loaded successfully!")
                is_valid, message = fa.validate()
                print(f"Validation: {message}")
            except Exception as e:
                print(f"Error during input: {e}")

        elif choice == '8':
            is_valid, message = fa.validate()
            print(f"\nValidation result: {message}")


        elif choice == '9':

            is_dfa, message = fa.is_deterministic()

            print(f"\nDFA Check: {message}")


        elif choice == '10':

            # First check if automaton is deterministic

            is_dfa, message = fa.is_deterministic()

            if not is_dfa:
                print(f"\nCannot check sequence: {message}")

                input("\nPress Enter to continue...")

                continue

            # Get sequence from user

            sequence = input("\nEnter sequence to check (e.g., '1010'): ").strip()

            result = fa.check_sequence(sequence)

            fa.display_sequence_check_result(sequence, result)



        elif choice == '11':

            # First check if automaton is deterministic

            is_dfa, message = fa.is_deterministic()

            if not is_dfa:
                print(f"\nCannot find prefix: {message}")

                input("\nPress Enter to continue...")

                continue

            # Get sequence from user

            sequence = input("\nEnter sequence to find longest accepted prefix: ").strip()

            result = fa.find_longest_accepted_prefix(sequence)

            fa.display_prefix_result(sequence, result)


        else:

            print("Invalid choice! Please enter a number between 0 and 11.")

        input("\nPress Enter to continue...")


if __name__ == "__main__":
    main()