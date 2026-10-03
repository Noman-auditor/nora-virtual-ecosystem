# Architecture

Tiers: 1 Normal APK, 2 Profile Owner, 3 Device Owner, 4 OEM/System, 5 Virtualization (AVF). Every capability returns a CapabilityResult; unavailable features return NoraResult.Failure with a FEATURE_* code. Public IP is always shown as 'Not directly determined'.
