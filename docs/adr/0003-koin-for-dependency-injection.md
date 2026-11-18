# Koin for dependency injection

The handoff suggested Hilt. We use Koin instead. Koin has no annotation processing, so builds are
faster and there is no KSP in every module. It is also straightforward to use from Glance widgets
and WorkManager workers, where Hilt needs hand-written entry points. In exchange we lose Hilt's
compile-time graph validation, so wiring errors appear at runtime. A Koin `verify()` unit test
must cover the modules.
