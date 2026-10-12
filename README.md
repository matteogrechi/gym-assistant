# Gym assistant

An exploratory mockup for an Android gym-session app. Android implementation has **not started**; scope must be decided first.

Open `mockup/index.html` in your browser, or serve this directory:

```sh
python3 -m http.server 8080
```

Then visit http://localhost:8080/mockup/ . On a phone, use the computer's LAN address and the same port.

Explore the sample session, select an exercise, edit seat notes and set weights/reps, and toggle completed sets. The video panel is explicitly a placeholder; real exercise video playback is deferred. Changes reset on reload. The mockup uses no build step or runtime dependencies. Optional Google Fonts require internet access; system fonts are the fallback.

- [Draft specification](docs/spec.md): requirements, acceptance criteria, assumptions, and excluded scope.
- [Bidirectional traceability](docs/traceability.md): requirement-to-code and code-to-requirement mappings, plus manual checks.

Every functional change must first be represented in the specification and then reflected in traceability. Prototype coverage is distinct from production delivery.
