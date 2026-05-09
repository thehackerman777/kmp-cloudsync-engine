#!/usr/bin/env python3
import os
# Root docs/index.html - redirects to web demo
html = """<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"><meta http-equiv="refresh" content="0;url=web-sample/"><title>CloudSync Engine</title></head>
<body><p><a href="web-sample/">Go to CloudSync Engine Web Demo</a></p></body>
</html>"""
os.makedirs("docs", exist_ok=True)
with open("docs/index.html", "w") as f:
    f.write(html)

# .nojekyll for GitHub Pages
with open("docs/.nojekyll", "w") as f:
    f.write("")

print("Root docs created")
