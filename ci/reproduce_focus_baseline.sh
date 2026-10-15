#!/usr/bin/env bash
set -eu
source_file=app/src/main/java/com/example/stbplay/ui/StbPlayUI.kt
restore_candidate() { git restore -- "$source_file"; }
trap restore_candidate EXIT
git show v2.10.26.5:"$source_file" > "$source_file"
./gradlew --no-daemon connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.stbplay.ui.TvNavigationFocusTest#searchKeepsFocusAndOpensWithRemoteAcrossSections,com.example.stbplay.ui.TvNavigationFocusTest#offscreenSavedCategoryListReturnsToAttachedTopCategory && baseline_exit=0 || baseline_exit=$?
mkdir -p "$RUNNER_TEMP/optimization-reports/baseline-reproduction"
cp -R app/build/outputs/androidTest-results "$RUNNER_TEMP/optimization-reports/baseline-reproduction/"
python3 - <<'CHECK'
import glob, xml.etree.ElementTree as ET
cases = [c for p in glob.glob('app/build/outputs/androidTest-results/connected/**/*.xml', recursive=True) for c in ET.parse(p).getroot().iter('testcase')]
expected = {'searchKeepsFocusAndOpensWithRemoteAcrossSections', 'offscreenSavedCategoryListReturnsToAttachedTopCategory'}
failed = {c.attrib['name'] for c in cases if c.find('failure') is not None}
assert failed == expected, (failed, expected)
assert len(cases) == 2, len(cases)
print('Confirmed both focus regressions fail against the unmodified 2.10.26.5 UI.')
CHECK
test "$baseline_exit" -ne 0
restore_candidate
trap - EXIT
