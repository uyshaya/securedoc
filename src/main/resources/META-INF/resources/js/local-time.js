// Browser-local time formatting for SecureDoc.
//
// Every date/instant rendered by the server reaches the page as an ISO-8601
// UTC string inside a <time datetime="...Z"> element -- for example:
//
//     <time datetime="#{staff.lastLogin}" class="local-time">
//         #{staff.lastLogin}
//     </time>
//
// This script walks every such element on DOMContentLoaded and replaces its
// text content with the same instant formatted in the user's browser
// timezone. The `datetime` attribute is preserved so JS / AT can re-read
// the canonical UTC value.
//
// Mode is controlled per-element via `data-format`:
//   data-format="datetime" -> "May 14, 2026, 4:00 PM"   (default)
//   data-format="date"     -> "May 14, 2026"
//   data-format="time"     -> "4:00 PM"
//   data-format="relative" -> "2 hours ago"
//
// Re-run for PrimeFaces partial updates by listening to pf:ajaxComplete.

(function () {
    'use strict';

    var DATETIME_OPTIONS = {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
    };
    var DATE_OPTIONS = { year: 'numeric', month: 'short', day: 'numeric' };
    var TIME_OPTIONS = { hour: '2-digit', minute: '2-digit' };

    function relativeFormat(instant) {
        var nowMs = Date.now();
        var diffMs = instant.getTime() - nowMs;
        var diffSec = Math.round(diffMs / 1000);
        var absSec = Math.abs(diffSec);

        var rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });
        if (absSec < 60) return rtf.format(diffSec, 'second');
        if (absSec < 3600) return rtf.format(Math.round(diffSec / 60), 'minute');
        if (absSec < 86400) return rtf.format(Math.round(diffSec / 3600), 'hour');
        if (absSec < 2592000) return rtf.format(Math.round(diffSec / 86400), 'day');
        if (absSec < 31536000) return rtf.format(Math.round(diffSec / 2592000), 'month');
        return rtf.format(Math.round(diffSec / 31536000), 'year');
    }

    function format(instant, mode) {
        switch (mode) {
            case 'date':     return instant.toLocaleDateString(undefined, DATE_OPTIONS);
            case 'time':     return instant.toLocaleTimeString(undefined, TIME_OPTIONS);
            case 'relative': return relativeFormat(instant);
            default:         return instant.toLocaleString(undefined, DATETIME_OPTIONS);
        }
    }

    function localize(root) {
        var elements = (root || document).querySelectorAll('time[datetime]');
        for (var index = 0; index < elements.length; index++) {
            var element = elements[index];
            var iso = element.getAttribute('datetime');
            if (!iso) continue;

            var instant = new Date(iso);
            if (isNaN(instant.getTime())) continue;

            var mode = element.getAttribute('data-format') || 'datetime';
            element.textContent = format(instant, mode);
            // Mirror the UTC value into a tooltip so users can see the canonical timestamp.
            if (!element.title) {
                element.title = iso;
            }
        }
    }

    function init() {
        localize(document);
        // PrimeFaces fires pf:ajaxComplete when partial updates land -- re-run.
        if (typeof window.PrimeFaces !== 'undefined') {
            document.addEventListener('pf:ajaxComplete', function (event) {
                localize(event.target || document);
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Exposed for any inline script that needs to localize freshly-injected DOM.
    window.SecureDocLocalTime = { localize: localize };
})();
