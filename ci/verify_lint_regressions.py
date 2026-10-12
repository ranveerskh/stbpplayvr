"""Fail on new lint errors, while reporting existing baseline findings explicitly."""
from collections import Counter
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def findings(path):
    root = ET.parse(path).getroot()
    errors = Counter()
    warnings = 0
    for issue in root.findall('issue'):
        if issue.get('severity') not in ('Error', 'Fatal'):
            warnings += 1
            continue
        locations = issue.findall('location')
        files = tuple(sorted(loc.get('file', '').replace('\\', '/').split('/app/')[-1]
                             for loc in locations))
        errors[(issue.get('id'), issue.get('message'), files)] += 1
    return errors, warnings


baseline, base_warnings = findings(sys.argv[1])
candidate, warnings = findings(sys.argv[2])
new = candidate - baseline
print(f'Baseline: {sum(baseline.values())} lint errors, {base_warnings} other findings')
print(f'Candidate: {sum(candidate.values())} lint errors, {warnings} other findings')
print(f'New lint errors: {sum(new.values())}')
for (issue_id, message, files), count in new.items():
    print(f'{issue_id} ({count}): {message} — {", ".join(files)}')
if new:
    sys.exit(1)
