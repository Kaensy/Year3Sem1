from typing import Dict, Set, List, Tuple
from collections import defaultdict


class Grammar:
    def __init__(self):
        self.productions: Dict[str, List[List[str]]] = defaultdict(list)
        self.terminals: Set[str] = set()
        self.non_terminals: Set[str] = set()
        self.start_symbol: str = None

    def add_production(self, non_terminal: str, production: List[str]):
        self.productions[non_terminal].append(production)
        self.non_terminals.add(non_terminal)
        for symbol in production:
            if not symbol.isupper() and symbol != 'epsilon':
                self.terminals.add(symbol)

    def read_from_file(self, filename: str):
        try:
            with open(filename, 'r') as f:
                lines = f.readlines()

            for line in lines:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue

                lhs, rhs = line.split('->')
                lhs = lhs.strip()

                for prod in rhs.split('|'):
                    production = [symbol.strip() for symbol in prod.strip().split()]
                    self.add_production(lhs, production)

                if self.start_symbol is None:
                    self.start_symbol = lhs

        except FileNotFoundError:
            raise Exception(f"Grammar file {filename} not found")

    def compute_first_sets(self) -> Dict[str, Set[str]]:
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

                    # Handle other productions
                    can_be_epsilon = True
                    for symbol in production:
                        # Add all non-epsilon symbols from first set
                        symbol_first = first[symbol] - {'epsilon'}
                        if not symbol_first.issubset(first[non_terminal]):
                            first[non_terminal].update(symbol_first)
                            changed = True

                        # If this symbol can't be epsilon, stop checking rest
                        if 'epsilon' not in first[symbol]:
                            can_be_epsilon = False
                            break

                    # If all symbols can be epsilon, add epsilon
                    if can_be_epsilon and 'epsilon' not in first[non_terminal]:
                        first[non_terminal].add('epsilon')
                        changed = True

        return first

    def compute_follow_sets(self, first_sets: Dict[str, Set[str]]) -> Dict[str, Set[str]]:
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
                            # Compute first of remaining sequence
                            remaining_first = set()
                            all_can_be_epsilon = True

                            for next_symbol in production[i + 1:]:
                                current_first = first_sets[next_symbol] - {'epsilon'}
                                remaining_first.update(current_first)
                                if 'epsilon' not in first_sets[next_symbol]:
                                    all_can_be_epsilon = False
                                    break

                            # Add first of remaining to follow of current
                            if not remaining_first.issubset(follow[symbol]):
                                follow[symbol].update(remaining_first)
                                changed = True

                            # If everything after can be epsilon or nothing after,
                            # add follow of LHS
                            if all_can_be_epsilon or i == len(production) - 1:
                                if not follow[non_terminal].issubset(follow[symbol]):
                                    follow[symbol].update(follow[non_terminal])
                                    changed = True

        return follow

    def build_parsing_table(self) -> Dict[Tuple[str, str], List[str]]:
        first_sets = self.compute_first_sets()
        follow_sets = self.compute_follow_sets(first_sets)
        parsing_table = {}

        for non_terminal in self.non_terminals:
            for production in self.productions[non_terminal]:
                first_of_prod = set()

                if production == ['epsilon']:
                    # For epsilon productions, add entry for all terminals in FOLLOW
                    for terminal in follow_sets[non_terminal]:
                        if (non_terminal, terminal) in parsing_table:
                            raise Exception(f"Grammar is not LL(1): Conflict at {(non_terminal, terminal)}")
                        parsing_table[(non_terminal, terminal)] = production
                else:
                    # Compute FIRST set of the production
                    can_be_epsilon = True
                    for symbol in production:
                        current_first = first_sets[symbol] - {'epsilon'}
                        first_of_prod.update(current_first)
                        if 'epsilon' not in first_sets[symbol]:
                            can_be_epsilon = False
                            break

                    # Add entries for each terminal in FIRST
                    for terminal in first_of_prod:
                        if (non_terminal, terminal) in parsing_table:
                            raise Exception(f"Grammar is not LL(1): Conflict at {(non_terminal, terminal)}")
                        parsing_table[(non_terminal, terminal)] = production

                    # If production can derive epsilon, add entries for FOLLOW
                    if can_be_epsilon:
                        for terminal in follow_sets[non_terminal]:
                            if (non_terminal, terminal) in parsing_table:
                                raise Exception(f"Grammar is not LL(1): Conflict at {(non_terminal, terminal)}")
                            parsing_table[(non_terminal, terminal)] = ['epsilon']

        return parsing_table

    def parse(self, input_string: str) -> List[List[str]]:
        parsing_table = self.build_parsing_table()
        stack = ['$', self.start_symbol]
        input_tokens = input_string.split() + ['$']
        position = 0
        derivation = []

        while stack:
            if not stack:
                if position == len(input_tokens):
                    break
                else:
                    raise Exception("Error: Stack empty but input remaining")

            top = stack[-1]
            current_token = input_tokens[position]

            if top in self.terminals or top == '$':
                if top == current_token:
                    stack.pop()
                    position += 1
                else:
                    raise Exception(f"Error: Expected {top}, got {current_token}")
            elif top in self.non_terminals:
                if (top, current_token) not in parsing_table:
                    raise Exception(f"Error: No production for {top} with {current_token}")

                production = parsing_table[(top, current_token)]
                stack.pop()
                derivation.append(production)

                if production != ['epsilon']:
                    for symbol in reversed(production):
                        stack.append(symbol)
            else:
                raise Exception(f"Error: Invalid symbol on stack: {top}")

        return derivation