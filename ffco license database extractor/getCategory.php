<?php
// getCategory.php
//
// INPUT (POST, minimal data transfer):
//   names[]=SAINT-MARCEL+Laurent&names[]=ANDRE+Jean ...
//   year=2026
//
// - Accepts application/x-www-form-urlencoded
// - Parameter name: "names" (can be repeated, as an array)
// - Parameter "year" (integer): selects ffco_licenses_<year>.json; if omitted, current calendar year is used
//
// OUTPUT:
//   JSON array of categories in the same order as input names, e.g.
//   ["H45","H75",null]
//   (null when category not found, or when the license file for that year is missing/unreadable/invalid)

header('Content-Type: application/json; charset=utf-8');

function json_escape_string($s) {
    $s = (string)$s;
    $repl = array(
        "\\" => "\\\\",
        "\"" => "\\\"",
        "\n" => "\\n",
        "\r" => "\\r",
        "\t" => "\\t",
        "\f" => "\\f",
        "\b" => "\\b",
    );
    return strtr($s, $repl);
}

function is_list_array($arr) {
    if (!is_array($arr)) return false;
    $i = 0;
    foreach ($arr as $k => $v) {
        if ($k !== $i) return false;
        $i++;
    }
    return true;
}

function json_encode_fallback($value) {
    if (is_null($value)) return "null";
    if ($value === true) return "true";
    if ($value === false) return "false";

    if (is_int($value) || is_float($value)) {
        // Keep JSON number format
        return (string)$value;
    }

    if (is_string($value)) {
        return "\"" . json_escape_string($value) . "\"";
    }

    if (is_array($value)) {
        if (is_list_array($value)) {
            $parts = array();
            foreach ($value as $v) {
                $parts[] = json_encode_fallback($v);
            }
            return "[" . implode(",", $parts) . "]";
        } else {
            $parts = array();
            foreach ($value as $k => $v) {
                $parts[] = json_encode_fallback((string)$k) . ":" . json_encode_fallback($v);
            }
            return "{" . implode(",", $parts) . "}";
        }
    }

    // For unexpected types, encode as string
    return "\"" . json_escape_string((string)$value) . "\"";
}

function json_encode_compat($value, $options = 0) {
    if (function_exists('json_encode')) {
        // Some very old builds might still have it missing; guarded above.
        return json_encode($value, $options);
    }
    // Options are ignored in fallback
    return json_encode_fallback($value);
}

function set_status_code($status_code) {
    $code = (int)$status_code;
    $texts = array(
        200 => 'OK',
        400 => 'Bad Request',
        401 => 'Unauthorized',
        403 => 'Forbidden',
        404 => 'Not Found',
        405 => 'Method Not Allowed',
        500 => 'Internal Server Error',
        502 => 'Bad Gateway',
    );
    $text = isset($texts[$code]) ? $texts[$code] : 'Error';

    if (isset($_SERVER['SERVER_PROTOCOL']) && $_SERVER['SERVER_PROTOCOL'] !== '') {
        header($_SERVER['SERVER_PROTOCOL'] . ' ' . $code . ' ' . $text);
    } else {
        header('HTTP/1.1 ' . $code . ' ' . $text);
    }
}

function respond_error($message, $status_code = 400) {
    set_status_code($status_code);
    $options = 0;
    if (defined('JSON_UNESCAPED_UNICODE')) {
        $options = $options | JSON_UNESCAPED_UNICODE;
    }
    echo json_encode_compat(array('error' => $message), $options);
    exit;
}

function unescape_json_string($s) {
    // Minimal unescape for common JSON escapes we might encounter in names
    // (handles \" \\ \/ \n \r \t \b \f)
    return stripcslashes($s);
}

function normalize_name_key($name) {
    // 1) Uppercase for case-insensitive matching
    $s = strtoupper((string)$name);

    // 2) Replace common accented/special latin chars with ASCII equivalents
    $map = array(
        'À' => 'A', 'Á' => 'A', 'Â' => 'A', 'Ã' => 'A', 'Ä' => 'A', 'Å' => 'A',
        'Æ' => 'AE',
        'Ç' => 'C',
        'È' => 'E', 'É' => 'E', 'Ê' => 'E', 'Ë' => 'E',
        'Ì' => 'I', 'Í' => 'I', 'Î' => 'I', 'Ï' => 'I',
        'Ñ' => 'N',
        'Ò' => 'O', 'Ó' => 'O', 'Ô' => 'O', 'Õ' => 'O', 'Ö' => 'O',
        'Œ' => 'OE',
        'Ù' => 'U', 'Ú' => 'U', 'Û' => 'U', 'Ü' => 'U',
        'Ý' => 'Y',
        'Ÿ' => 'Y'
    );
    $s = strtr($s, $map);

    // 3) Keep only English alphabet letters A-Z
    $s = preg_replace('/[^A-Z]/', '', $s);

    return $s;
}

function parse_request_payload_compat($rawInput) {
    // If json_decode exists, prefer it.
    if (function_exists('json_decode')) {
        $payload = json_decode($rawInput, true);
        if (is_array($payload)) {
            return $payload;
        }
        return null;
    }

    // Fallback: very small parser for our expected structure:
    // { "runners": [ { "i": 1, "n": "NAME" }, ... ] }
    // or { "runners": [ { "id": "1090", "name": "NAME" }, ... ] }
    //
    // We extract objects inside the runners array and then pick id/i and name/n.
    $runners = array();

    // Try to isolate the runners array contents
    if (!preg_match('/"runners"\s*:\s*\[(.*)\]\s*}/s', $rawInput, $m)) {
        return null;
    }
    $arr = $m[1];

    // Extract each {...} object (non-nested, which matches our payload)
    if (!preg_match_all('/\{[^{}]*\}/s', $arr, $objs)) {
        return array('runners' => array());
    }

    foreach ($objs[0] as $obj) {
        $id = null;
        $name = null;

        if (preg_match('/"(?:i|id)"\s*:\s*"?(\d+)"?/s', $obj, $mi)) {
            $id = $mi[1];
        }
        if (preg_match('/"(?:n|name)"\s*:\s*"((?:\\\\.|[^"\\\\])*)"/s', $obj, $mn)) {
            $name = unescape_json_string($mn[1]);
        }

        if ($id !== null || $name !== null) {
            // Normalize to keys used later: i/n
            $row = array();
            if ($id !== null) $row['i'] = (int)$id;
            if ($name !== null) $row['n'] = (string)$name;
            $runners[] = $row;
        }
    }

    return array('runners' => $runners);
}

function parse_ffco_licenses_by_name_compat($jsonContent) {
    // If json_decode exists, prefer it.
    if (function_exists('json_decode')) {
        $data = json_decode($jsonContent, true);
        if (!is_array($data) || !isset($data['runners']) || !is_array($data['runners'])) {
            return null;
        }

        $byName = array();
        foreach ($data['runners'] as $runner) {
            if (!isset($runner['runnerName']) || !isset($runner['category'])) {
                continue;
            }
            $k = normalize_name_key((string)$runner['runnerName']);
            $byName[$k] = (string)$runner['category'];
        }
        return $byName;
    }

    // Fallback: parse runner objects from JSON text with regex.
    // We only need runnerName and category.
    $byName = array();

    // Match each object that contains "runnerName": "...", ... "category": "..."
    if (preg_match_all('/\{\s*"runnerName"\s*:\s*"((?:\\\\.|[^"\\\\])*)"\s*,.*?"category"\s*:\s*"((?:\\\\.|[^"\\\\])*)".*?\}/s', $jsonContent, $matches, PREG_SET_ORDER)) {
        foreach ($matches as $m) {
            $name = unescape_json_string($m[1]);
            $cat = unescape_json_string($m[2]);
            $k = normalize_name_key((string)$name);
            $byName[$k] = (string)$cat;
        }
    }

    return $byName;
}

// -----------------------
// MAIN LOGIC (simplified)
// -----------------------

// Read names from POST (application/x-www-form-urlencoded):
// names[]=A&names[]=B...
$names = array();
if (isset($_POST['names'])) {
    if (is_array($_POST['names'])) {
        $names = $_POST['names'];
    } else {
        $names = array($_POST['names']);
    }
}

// If no input, default to one sample name as requested
if (count($names) === 0) {
    $names = array('SAINT-MARCEL Laurent');
}

$year = (int)date('Y');
if (isset($_POST['year']) && $_POST['year'] !== '') {
    $year = (int)$_POST['year'];
}
if ($year < 2000 || $year > 2100) {
    respond_error('Invalid year: must be between 2000 and 2100.', 400);
}

$jsonPath = 'ffco_licenses_' . $year . '.json';
$byName = array();
if (file_exists($jsonPath)) {
    $jsonContent = file_get_contents($jsonPath);
    if ($jsonContent !== false) {
        $parsed = parse_ffco_licenses_by_name_compat($jsonContent);
        if ($parsed !== null) {
            $byName = $parsed;
        }
    }
}

// Build categories array in same order as input names
$categories = array();
foreach ($names as $n) {
    $nameKey = normalize_name_key((string)$n);
    if (array_key_exists($nameKey, $byName)) {
        $categories[] = $byName[$nameKey];
    } else {
        $categories[] = null;
    }
}

$options = 0;
if (defined('JSON_UNESCAPED_UNICODE')) {
    $options = $options | JSON_UNESCAPED_UNICODE;
}
// Pretty-print is optional but doesn't add much data; enable if available.
if (defined('JSON_PRETTY_PRINT')) {
    $options = $options | JSON_PRETTY_PRINT;
}

// Minimal response: JSON array of categories
echo json_encode_compat($categories, $options);

?>
