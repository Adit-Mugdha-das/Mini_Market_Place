(function () {
    "use strict";

    var emojiMap = {
        "\u2699\uFE0F": "settings",
        "\u2699": "settings",
        "\uD83D\uDC64": "account_circle",
        "\uD83D\uDED2": "shopping_cart",
        "\uD83C\uDFEA": "storefront",
        "\uD83D\uDCB3": "credit_card",
        "\uD83D\uDEDD\uFE0F": "shopping_bag",
        "\uD83D\uDED5\uFE0F": "shopping_bag",
        "\uD83D\uDED6\uFE0F": "shopping_bag",
        "\uD83D\uDCE6": "inventory_2",
        "\u2B50": "star",
        "\u26A0\uFE0F": "warning",
        "\u26A0": "warning",
        "\uD83D\uDC4B": "waving_hand",
        "\u2705": "check_circle",
        "\u2714": "check",
        "\uD83D\uDE9A": "local_shipping",
        "\u274C": "cancel",
        "\u274E": "cancel",
        "\uD83D\uDD12": "lock",
        "\u23F3": "schedule",
        "\uD83D\uDD0D": "search",
        "\uD83C\uDF89": "celebration",
        "\uD83D\uDC65": "groups",
        "\uD83D\uDCAC": "chat",
        "\uD83D\uDCB0": "payments",
        "\uD83D\uDD11": "key",
        "\uD83D\uDCDD": "receipt_long",
        "\uD83C\uDFE6": "account_balance",
        "\uD83C\uDD7F\uFE0F": "payments",
        "\uD83D\uDCF1": "smartphone",
        "\uD83D\uDCF2": "smartphone",
        "\uD83D\uDCB5": "payments",
        "\uD83C\uDFF7\uFE0F": "sell",
        "\u2605": "star"
    };

    var emojis = Object.keys(emojiMap)
        .sort(function (a, b) { return b.length - a.length; })
        .map(function (k) {
            return k.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
        })
        .join("|");

    var emojiRegex = new RegExp(emojis, "g");

    function createIcon(name) {
        var span = document.createElement("span");
        span.className = "material-symbols-outlined";
        span.setAttribute("aria-hidden", "true");
        span.textContent = name;
        return span;
    }

    function convertTextNode(node) {
        var text = node.nodeValue;
        if (!text || !emojiRegex.test(text)) {
            return;
        }
        emojiRegex.lastIndex = 0;

        var frag = document.createDocumentFragment();
        var last = 0;
        text.replace(emojiRegex, function (match, offset) {
            if (offset > last) {
                frag.appendChild(document.createTextNode(text.slice(last, offset)));
            }
            frag.appendChild(createIcon(emojiMap[match] || "adjust"));
            last = offset + match.length;
            return match;
        });

        if (last < text.length) {
            frag.appendChild(document.createTextNode(text.slice(last)));
        }

        node.parentNode.replaceChild(frag, node);
    }

    function convertEmojis(root) {
        var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
        var nodes = [];
        while (walker.nextNode()) {
            var current = walker.currentNode;
            var parentTag = current.parentNode && current.parentNode.nodeName;
            if (parentTag === "SCRIPT" || parentTag === "STYLE") {
                continue;
            }
            nodes.push(current);
        }

        nodes.forEach(convertTextNode);
    }

    function normalizeBrand() {
        var brands = document.querySelectorAll(".brand, .brand-name");
        brands.forEach(function (el) {
            var raw = (el.textContent || "").trim();
            if (!raw || raw.indexOf("Haat") === -1 || el.querySelector("img")) {
                return;
            }

            raw = raw.replace(emojiRegex, "").replace(/\s+/g, " ").trim();
            var label = raw || "HaatBazar";
            el.textContent = "";
            el.classList.add("hb-brand");

            var img = document.createElement("img");
            img.className = "hb-brand-icon";
            img.src = "/images/Haat_Bazar-logo.png";
            img.alt = "Haat Bazar logo";

            var text = document.createElement("span");
            text.textContent = label;

            el.appendChild(img);
            el.appendChild(text);
        });
    }

    function init() {
        normalizeBrand();
        convertEmojis(document.body);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
