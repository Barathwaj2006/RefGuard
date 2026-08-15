import re

with open("README.md", "r") as f:
    content = f.read()

# Try to find the conflict markers
if "<<<<<<< HEAD" in content:
    # We want to keep the HEAD version (mvp-integration) which is better formatted
    # but we will just remove the conflict markers and the other branch content entirely.
    # Alternatively we can extract the head portion.
    head_start = content.find("<<<<<<< HEAD\n")
    sep = content.find("\n=======\n")
    tail_end = content.find("\n>>>>>>> origin/refguard/backend")
    tail_end = content.find("\n", tail_end + 1)

    if head_start != -1 and sep != -1 and tail_end != -1:
        head_content = content[head_start + len("<<<<<<< HEAD\n"):sep]

        # also check if the other stuff is appended
        new_content = content[:head_start] + head_content + content[tail_end:]

        with open("README.md", "w") as f:
            f.write(new_content)
        print("Fixed README.md conflicts")
    else:
        print("Could not find markers perfectly")
else:
    print("No conflict markers found")
