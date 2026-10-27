# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- An option to only work on the lock screen, so apps get the volume button as usual once the phone is unlocked.

### Changed
- While a camera is in use, the volume button goes to the app untouched, so it still works as a camera shutter.
- While the torch is on, a double-press always turns it off, even in those cases.

## [1.0.0] - 2026-10-27

### Added
- Double-press volume up or volume down to toggle the torch, including on the lock screen.
- Optional screen-off support (experimental). It steps aside while audio plays or a call is on.
- Guided setup for the accessibility service, with a floating guide over Android's settings and help for the "Restricted setting" block.
- An on/off switch, a button to test the torch, and an About screen.
