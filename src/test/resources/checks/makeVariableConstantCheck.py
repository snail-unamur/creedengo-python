timeout = 10       # Noncompliant {{Make this variable a constant by renaming it to uppercase}}

def time():
    print(timeout)

MAX_RETRY = 3     # Compliant

value = 1
value = 2         # Compliant

api_url = "x"     # Noncompliant {{Make this variable a constant by renaming it to uppercase}}



counter = 0
counter += 1       # Compliant
_hidden = 3        # Compliant