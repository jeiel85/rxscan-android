# Release and Compliance Plan

## 1. Intended-use discipline

Recommended intended-use statement:

> This application helps users digitize printed pharmacy dispensing-bag information and look up official medicine information. It does not diagnose, prescribe, recommend dose changes, or replace professional medical advice.

Engineering, store listing, screenshots, onboarding, and support responses must align with the same intended use.

## 2. Regulatory review checkpoint

Before a public beta, submit the actual feature specification, screenshots, matching logic, wording, and intended-use statement for professional review of Korean medical-device/digital-medical-product applicability.

Do not assume that a disclaimer alone determines classification. Functional behavior and claims matter.

## 3. Google Play

Complete according to actual behavior:

- Health apps declaration;
- Data safety form;
- privacy policy on a stable public URL;
- content rating;
- camera permission disclosure;
- medical/health disclaimer required by applicable policy;
- target API and SDK declarations current at release time.

The bundle intentionally does not hard-code a future target SDK; CI must enforce the latest stable SDK and current Play requirement at release time.

## 4. Privacy policy minimum contents

- controller/developer identity and contact;
- exact data processed locally;
- no-account behavior;
- network endpoints and purposes;
- third-party SDKs;
- storage and retention defaults;
- image deletion behavior;
- backup behavior;
- user deletion/export rights;
- security safeguards;
- incident contact;
- child/family use considerations;
- changes and effective date.

## 5. Public-data governance

For every source:

- record official dataset title and agency;
- retain URL and access date;
- record license/usage scope;
- verify redistribution of transformed database and images;
- include required attribution;
- display source update date;
- maintain change history.

A “no restriction” portal field still requires preserving provenance and reviewing referenced terms.

## 6. Clinical content review

A licensed pharmacist should review:

- simplified fixed copy;
- DUR warning templates;
- missing-data messaging;
- emergency/side-effect boundaries;
- prescribed-vs-approved information separation;
- terminology for dosage forms and routes;
- user correction UX.

Review decisions are versioned, signed off, and mapped to app/content versions.

## 7. Store release stages

1. internal test with synthetic data;
2. closed test with trained testers and consented de-identified samples;
3. limited safety beta;
4. production staged rollout;
5. expand only after monitoring non-sensitive operational metrics and support reports.

Without analytics, collect operational quality through:

- local self-test;
- opt-in redacted diagnostic export;
- manual support reports;
- store crash/ANR platform summaries configured to avoid attachments containing health data;
- controlled QA corpus runs.

## 8. Support policy

Support must never ask users to email an unredacted prescription by default.

Preferred workflow:

1. ask for app version, DB version, error code, and pharmacy template category;
2. offer local redacted diagnostic export;
3. instruct user how to mask identity and medicine data when only layout debugging is needed;
4. use a separate explicit consent process for any health-data sample.

## 9. Launch blockers

- legal/regulatory status unresolved;
- no pharmacist review;
- artifact signing/rollback untested;
- wrong high-confidence match in release holdout;
- private data found in network/log/backup;
- source licensing/attribution unresolved;
- stale data pipeline with no operational response;
- missing incident response owner;
- marketing claims exceed intended use.
