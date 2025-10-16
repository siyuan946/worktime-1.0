# Record Upload Page Review

## Frontend Parsing Flow
- `RecordUpload.vue` posts the selected file to `/api/workrecords/import` and immediately switches into a loading state (`loading`, `importing`, progress bar). The component then starts polling `/status` for the returned `jobId` until the backend finishes or fails. 【F:frontend/src/components/RecordUpload.vue†L517-L606】
- Once the status endpoint reports `COMPLETED`, the component reloads file metadata and the first page of parsed records, clears the file picker, and hides the progress UI. The rendered table pulls data from `pageRecords` and lazy-loads barcode images for the current page so the preview updates automatically after the job finishes. 【F:frontend/src/components/RecordUpload.vue†L587-L753】

## Display Logic
- Pagination is keyed off the selected drawing number and page (`currentDrawingIndex`, `pageNo`). Data is fetched via `/file/{fileId}/page` and normalized through `decorateRecord`, which also flags rows missing codes or hours so the UI can highlight them. 【F:frontend/src/components/RecordUpload.vue†L339-L488】
- The preview table and print layout rely on `pageRecords` and the derived `printPages` array. Barcode images are fetched lazily to minimize payload size. 【F:frontend/src/components/RecordUpload.vue†L37-L196】【F:frontend/src/components/RecordUpload.vue†L920-L998】

## Backend Review
- The repository does not currently include the backend source under `backend/`, so the implementation of `/api/workrecords/import` and the related status endpoints cannot be inspected here.

## Recent Frontend Enhancements
- The upload UI now surfaces textual status updates beneath the progress bar, clears the file input after a successful import, and resets polling state before starting a new job so operators see a clean status each time. 【F:frontend/src/components/RecordUpload.vue†L30-L57】【F:frontend/src/components/RecordUpload.vue†L554-L623】
- Polling consumes any `totalRows` value the backend returns to compute a real completion percentage, falls back to the previous logarithmic estimate when totals are unavailable, and stops after repeated network errors with a visible message. 【F:frontend/src/components/RecordUpload.vue†L623-L688】

## Potential Improvements
- Consider reusing barcode images that the backend might return with each record instead of regenerating them per page to reduce extra requests, provided the backend exposes cached images.
- Expose a cancel button so long-running jobs can be aborted without refreshing the page once the backend supports cancellation.

## Parsing Mode
- Because the component submits the file and then polls for completion instead of waiting for the upload request to return parsed rows, the current design is **asynchronous** from the frontend’s perspective.
