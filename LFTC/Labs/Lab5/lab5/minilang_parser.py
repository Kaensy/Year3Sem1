from typing import Dict, Set, List, Tuple, Optional
from collections import defaultdict
from dataclasses import dataclass, field


@dataclass
class FIPEntry:
    """Represents an entry in the FIP (Forma Internă a Programului)"""
    token: str
    code: int
    symbol_table_pos: int


@dataclass
class ParserContext:
    """Tracks parsing context including scope and symbols"""
    current_scope: str = "global"
    symbol_table: Dict[str, Dict] = field(default_factory=dict)
    errors: List[str] = field(default_factory=list)


class MinilangParser:
    def __init__(self):
        self.productions: Dict[str, List[List[str]]] = defaultdict(list)
        self.terminals: Set[str] = set()
        self.non_terminals: Set[str] = set()
        self.start_symbol: str = None
        self.parsing_table: Dict[Tuple[str, str], List[str]] = {}
        self.context = ParserContext()
        self.stack = []  # Added to track parsing stack

        # Extended token codes including division
        self.token_codes = {
            'int': 2, 'double': 3, 'void': 4, 'main': 5,
            'cout': 6, 'cin': 7, 'while': 8, 'if': 9,
            'else': 10, 'M_PI': 11, 'endl': 12,
            '+': 13, '-': 14, '*': 15, '/': 16, '<<': 17, '>>': 18,
            '=': 19, '!=': 20, '>': 21, '<': 22, '<=': 23,
            '>=': 24, '==': 25, '[': 26, ']': 27,
            '(': 28, ')': 29, '{': 30, '}': 31,
            ',': 32, ';': 33, ' ': 34
        }

    def format_error_context(self, position: int, fip_entries: List[FIPEntry], context_size: int = 3) -> str:
        """Format error context showing nearby tokens"""
        context_lines = []
        start = max(0, position - context_size)
        end = min(len(fip_entries), position + context_size + 1)

        for i in range(start, end):
            prefix = "-> " if i == position else "   "
            entry = fip_entries[i]
            context_lines.append(f"{prefix}{entry.token:15} (line position {i})")

        return "\n".join(context_lines)

    def handle_parsing_error(self, position: int, fip_entries: List[FIPEntry], message: str) -> str:
        """Create detailed error message with context"""
        error_msg = f"\nSyntax error at position {position}:\n"
        error_msg += self.format_error_context(position, fip_entries)
        error_msg += f"\n{message}"

        # Add stack trace if available
        if self.stack:
            error_msg += f"\nParsing stack: {self.stack}"

        return error_msg

    def read_grammar(self, filename: str):
        """Read grammar rules from file"""
        try:
            with open(filename, 'r') as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith('#'):
                        continue

                    lhs, rhs = line.split('->')
                    lhs = lhs.strip()

                    # Add production rules
                    for prod in rhs.split('|'):
                        production = [sym.strip() for sym in prod.strip().split()]
                        self.add_production(lhs, production)

                    # Set first non-terminal as start symbol
                    if self.start_symbol is None:
                        self.start_symbol = lhs

            # After reading grammar, build parsing table
            self.parsing_table = self.build_parsing_table()
            # Validate the parsing table
            self.validate_parsing_table()

        except FileNotFoundError:
            raise Exception(f"Grammar file {filename} not found")
        except Exception as e:
            raise Exception(f"Error reading grammar: {str(e)}")

    def add_production(self, non_terminal: str, production: List[str]):
        """Add a production rule to the grammar"""
        self.productions[non_terminal].append(production)
        self.non_terminals.add(non_terminal)
        for symbol in production:
            if symbol != 'epsilon' and symbol not in self.non_terminals:
                self.terminals.add(symbol)

    def read_fip(self, filename: str) -> List[FIPEntry]:
        """Read FIP from file"""
        entries = []
        try:
            with open(filename, 'r') as f:
                # Skip header lines
                next(f)  # Skip header
                next(f)  # Skip separator line

                for line in f:
                    if line.strip():
                        parts = line.strip().split('|')
                        token = parts[0].strip()
                        code = int(parts[1].strip())
                        symbol_pos = int(parts[2].strip()) if parts[2].strip() != "-" else -1
                        entries.append(FIPEntry(token, code, symbol_pos))
            return entries
        except FileNotFoundError:
            raise Exception(f"FIP file {filename} not found")
        except Exception as e:
            raise Exception(f"Error reading FIP file: {str(e)}")

    def parse_fip(self, fip_entries: List[FIPEntry]) -> List[List[str]]:
        """Parse input from FIP entries with better error handling"""
        self.stack = ['$', self.start_symbol]  # Initialize stack
        position = 0
        derivation = []

        print("\nParsing Table Contents:")
        for (nt, terminal), production in self.parsing_table.items():
            print(f"{nt}, {terminal} -> {' '.join(production)}")

        try:
            while self.stack and position < len(fip_entries):
                top = self.stack[-1]
                current_entry = fip_entries[position]
                current_token = current_entry.token

                print(f"\nStack: {self.stack}")
                print(f"Current token: {current_token} at position {position}")

                # Special handling for identifiers and numbers
                if top == 'id' and current_entry.code == 0:
                    self.stack.pop()
                    position += 1
                    continue
                elif top == 'number' and current_entry.code == 1:
                    self.stack.pop()
                    position += 1
                    continue

                # Handle terminals
                if top == current_token:
                    self.stack.pop()
                    position += 1
                elif top in self.non_terminals:
                    # Handle non-terminals
                    lookup_token = current_token
                    if current_entry.code == 0:
                        lookup_token = 'id'
                    elif current_entry.code == 1 or current_token.replace('.', '', 1).isdigit():
                        lookup_token = 'number'

                    if (top, lookup_token) not in self.parsing_table:
                        error_msg = f"No production for non-terminal '{top}' with token '{lookup_token}'"
                        raise Exception(self.handle_parsing_error(position, fip_entries, error_msg))

                    production = self.parsing_table[(top, lookup_token)]
                    print(f"Using production: {top} -> {' '.join(production)}")

                    self.stack.pop()
                    if production != ['epsilon']:
                        for symbol in reversed(production):
                            self.stack.append(symbol)
                    derivation.append(production)
                else:
                    error_msg = f"Expected '{top}', got '{current_token}'"
                    raise Exception(self.handle_parsing_error(position, fip_entries, error_msg))

            # Check for completion
            if self.stack != ['$'] or position < len(fip_entries):
                remaining = [e.token for e in fip_entries[position:]]
                raise Exception(f"Incomplete parse. Stack: {self.stack}, Remaining tokens: {remaining}")

            return derivation

        except Exception as e:
            # Add more context to the error
            error_msg = str(e)
            if "Syntax error" not in error_msg:
                error_msg = self.handle_parsing_error(position, fip_entries, error_msg)
            raise Exception(error_msg)

    def compute_first_sets(self) -> Dict[str, Set[str]]:
        """Compute FIRST sets for all symbols"""
        first = defaultdict(set)

        # Initialize terminals
        for terminal in self.terminals | {'epsilon'}:
            first[terminal] = {terminal}

        # Initialize non-terminals
        for non_terminal in self.non_terminals:
            first[non_terminal] = set()

        changed = True
        while changed:
            changed = False
            for non_terminal in self.non_terminals:
                for production in self.productions[non_terminal]:
                    # Handle epsilon production
                    if production == ['epsilon']:
                        if 'epsilon' not in first[non_terminal]:
                            first[non_terminal].add('epsilon')
                            changed = True
                        continue

                    # Compute FIRST set for the production
                    can_be_epsilon = True
                    first_of_prod = set()

                    for symbol in production:
                        symbol_first = first[symbol] - {'epsilon'}
                        first_of_prod.update(symbol_first)

                        if 'epsilon' not in first[symbol]:
                            can_be_epsilon = False
                            break

                    # Update FIRST set of non-terminal
                    if not first_of_prod.issubset(first[non_terminal]):
                        first[non_terminal].update(first_of_prod)
                        changed = True

                    if can_be_epsilon and 'epsilon' not in first[non_terminal]:
                        first[non_terminal].add('epsilon')
                        changed = True

        return first

    def compute_follow_sets(self, first_sets: Dict[str, Set[str]]) -> Dict[str, Set[str]]:
        """Compute FOLLOW sets for all non-terminals"""
        follow = defaultdict(set)
        follow[self.start_symbol].add('$')

        changed = True
        while changed:
            changed = False
            for non_terminal in self.non_terminals:
                for production in self.productions[non_terminal]:
                    if production == ['epsilon']:
                        continue

                    for i, symbol in enumerate(production):
                        if symbol in self.non_terminals:
                            # Calculate FIRST of remaining sequence
                            remaining_first = set()
                            all_can_be_epsilon = True

                            for next_symbol in production[i + 1:]:
                                symbol_first = first_sets[next_symbol] - {'epsilon'}
                                remaining_first.update(symbol_first)
                                if 'epsilon' not in first_sets[next_symbol]:
                                    all_can_be_epsilon = False
                                    break

                            # Add FIRST of remaining to FOLLOW of current
                            if not remaining_first.issubset(follow[symbol]):
                                follow[symbol].update(remaining_first)
                                changed = True

                            # If all remaining symbols can be epsilon or we're at the end,
                            # add FOLLOW of LHS to FOLLOW of current symbol
                            if all_can_be_epsilon or i == len(production) - 1:
                                if not follow[non_terminal].issubset(follow[symbol]):
                                    follow[symbol].update(follow[non_terminal])
                                    changed = True

        return follow

    def build_parsing_table(self) -> Dict[Tuple[str, str], List[str]]:
        """Build LL(1) parsing table"""
        first_sets = self.compute_first_sets()
        follow_sets = self.compute_follow_sets(first_sets)
        parsing_table = {}

        for non_terminal in self.non_terminals:
            for production in self.productions[non_terminal]:
                first_of_prod = set()
                can_be_epsilon = True

                if production == ['epsilon']:
                    # For epsilon productions, add entry for all terminals in FOLLOW
                    for terminal in follow_sets[non_terminal]:
                        if (non_terminal, terminal) in parsing_table:
                            raise Exception(f"Grammar is not LL(1): Conflict at {(non_terminal, terminal)}")
                        parsing_table[(non_terminal, terminal)] = production
                else:
                    # Compute FIRST set of the production
                    for symbol in production:
                        symbol_first = first_sets[symbol] - {'epsilon'}
                        first_of_prod.update(symbol_first)
                        if 'epsilon' not in first_sets[symbol]:
                            can_be_epsilon = False
                            break

                    # Add entries for each terminal in FIRST
                    for terminal in first_of_prod:
                        if (non_terminal, terminal) in parsing_table:
                            existing_prod = parsing_table[(non_terminal, terminal)]
                            if existing_prod != production:
                                raise Exception(f"Grammar is not LL(1): Conflict at {(non_terminal, terminal)} "
                                                f"between {existing_prod} and {production}")
                        parsing_table[(non_terminal, terminal)] = production

                    # If production can derive epsilon, add entries for FOLLOW
                    if can_be_epsilon:
                        for terminal in follow_sets[non_terminal]:
                            if (non_terminal, terminal) in parsing_table:
                                raise Exception(f"Grammar is not LL(1): Conflict at {(non_terminal, terminal)}")
                            parsing_table[(non_terminal, terminal)] = production

        return parsing_table

    def validate_parsing_table(self) -> List[str]:
        """Validate parsing table for completeness and conflicts"""
        issues = []

        # Check for missing entries
        for non_terminal in self.non_terminals:
            for terminal in self.terminals | {'$'}:
                if (non_terminal, terminal) not in self.parsing_table:
                    issues.append(f"Missing entry for ({non_terminal}, {terminal})")

        # Check for conflicts
        entries = defaultdict(list)
        for (nt, term), prod in self.parsing_table.items():
            entries[(nt, term)].append(prod)
            if len(entries[(nt, term)]) > 1:
                issues.append(f"Conflict at ({nt}, {term}): {entries[(nt, term)]}")

        return issues


def main():
    # Create parser instance
    parser = MinilangParser()

    # Read grammar
    print("Reading grammar...")
    parser.read_grammar("minilang_grammar.txt")

    # Validate parsing table
    issues = parser.validate_parsing_table()
    if issues:
        print("\nWarning: Found issues in parsing table:")
        for issue in issues:
            print(f"- {issue}")

    # Read FIP
    print("\nReading FIP...")
    try:
        fip_entries = parser.read_fip("fip_output.txt")
        print(f"Read {len(fip_entries)} FIP entries")

        # Parse
        print("\nParsing program...")
        derivation = parser.parse_fip(fip_entries)

        print("\nParsing successful!")
        print("\nDerivation steps:")
        for i, step in enumerate(derivation, 1):
            print(f"{i:3d}. {' '.join(step)}")

    except Exception as e:
        print(f"Error: {str(e)}")


if __name__ == "__main__":
    main()