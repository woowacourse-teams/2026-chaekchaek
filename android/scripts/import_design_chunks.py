"""Figma MCP 분할 응답을 검증하고 손실 없는 원본 JSON으로 복원한다."""
import argparse
import base64
import json
from pathlib import Path
import struct

from design_guard import validate_snapshot, write


def decode_chunks(chunks):
    chunks = sorted(chunks, key=lambda chunk: chunk["offset"])
    if not chunks:
        raise ValueError("원본 묶음 없음")
    expected_offset = 0
    for chunk in chunks:
        if chunk["offset"] != expected_offset:
            raise ValueError("누락 또는 중복된 원본 묶음")
        if any(chunk[key] != chunks[0][key] for key in ("total", "checksum", "nodes", "encoding")):
            raise ValueError("수집 도중 원본 변경")
        expected_offset += len(chunk["chunk"])
    if expected_offset != chunks[0]["total"] or chunks[0]["encoding"] != "lzw16-base64":
        raise ValueError("잘린 원본 또는 지원하지 않는 인코딩")
    encoded = "".join(chunk["chunk"] for chunk in chunks)
    checksum = 2166136261
    for char in encoded:
        checksum = ((checksum ^ ord(char)) * 16777619) & 0xffffffff
    if checksum != chunks[0]["checksum"]:
        raise ValueError("전송 중 원본 손상")
    binary = base64.b64decode(encoded, validate=True)
    codes = [value[0] for value in struct.iter_unpack(">H", binary)]
    dictionary = [bytes([n]) for n in range(256)]
    previous = dictionary[codes[0]]
    decoded = [previous]
    for code in codes[1:]:
        if code > len(dictionary):
            raise ValueError("잘못된 압축 코드")
        word = dictionary[code] if code < len(dictionary) else previous + previous[:1]
        decoded.append(word)
        if len(dictionary) < 65536:
            dictionary.append(previous + word[:1])
        previous = word
    snapshot = json.loads(b"".join(decoded).decode("utf-8"))
    validate_snapshot(snapshot, snapshot["roots"])
    if len(snapshot["records"]) != chunks[0]["nodes"]:
        raise ValueError("전송된 노드 수 불일치")
    return snapshot


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("chunks", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    write(args.output, decode_chunks(json.loads(args.chunks.read_text())))
