<?php
// proxy.php
//
// Simple proxy to inscriptions.co-paca.info to avoid CORS issues.
// This script must be on the same origin as licenseExtractor.html.
//
// It enforces a whitelist of remote URLs to avoid arbitrary proxying.

header('Content-Type: text/html; charset=utf-8');

// Optional: limit who can call this proxy (same origin / your own front-end)
// header('Access-Control-Allow-Origin: https://your-domain.example'); // adjust if needed

$type = isset($_GET['type']) ? $_GET['type'] : '';
$competitionId = isset($_GET['competitionId']) ? $_GET['competitionId'] : '';
$clubId = isset($_GET['clubId']) ? $_GET['clubId'] : '';

$base = 'https://inscriptions.co-paca.info';

function fetch_remote_get($url) {
    $opts = [
        'http' => [
            'method' => 'GET',
            'header' => "User-Agent: FFCO-Extractor/1.0\r\n",
            'timeout' => 15,
        ]
    ];
    $ctx = stream_context_create($opts);
    $result = @file_get_contents($url, false, $ctx);
    if ($result === false) {
        http_response_code(502);
        echo "Error fetching remote URL (GET).";
        exit;
    }
    echo $result;
    exit;
}

function fetch_remote_post($url, $postFields) {
    $opts = [
        'http' => [
            'method'  => 'POST',
            'header'  => "Content-Type: application/x-www-form-urlencoded\r\n"
                        . "User-Agent: FFCO-Extractor/1.0\r\n",
            'content' => http_build_query($postFields),
            'timeout' => 15,
        ]
    ];
    $ctx = stream_context_create($opts);
    $result = @file_get_contents($url, false, $ctx);
    if ($result === false) {
        http_response_code(502);
        echo "Error fetching remote URL (POST).";
        exit;
    }
    echo $result;
    exit;
}

if ($type === 'main') {
    // https://inscriptions.co-paca.info/
    $url = $base . '/';
    fetch_remote_get($url);
} elseif ($type === 'clubs') {
    // https://inscriptions.co-paca.info/inscription1.php?no={competitionId}
    if ($competitionId === '') {
        http_response_code(400);
        echo "Missing competitionId.";
        exit;
    }
    $url = $base . '/inscription1.php?no=' . urlencode($competitionId);
    fetch_remote_get($url);
} elseif ($type === 'runners') {
    // POST https://inscriptions.co-paca.info/inscription2.php?no={competitionId} with club={clubId}
    if ($competitionId === '' || $clubId === '') {
        http_response_code(400);
        echo "Missing competitionId or clubId.";
        exit;
    }
    $url = $base . '/inscription2.php?no=' . urlencode($competitionId);
    fetch_remote_post($url, ['club' => $clubId]);
} else {
    http_response_code(400);
    echo "Invalid proxy type.";
    exit;
}

