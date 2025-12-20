#ifndef MANAGER_SIGN_H
#define MANAGER_SIGN_H

// ShirkNeko/SukiSU
#define EXPECTED_SIZE_SHIRKNEKO 0x35c
#define EXPECTED_HASH_SHIRKNEKO                                                \
    "947ae944f3de4ed4c21a7e4f7953ecf351bfa2b36239da37a34111ad29993eef"

// ReSukiSU/ReSukiSU
#define EXPECTED_SIZE_RESUKISU 0x377
#define EXPECTED_HASH_RESUKISU                                                 \
    "d3469712b6214462764a1d8d3e5cbe1d6819a0b629791b9f4101867821f1df64"

// Dynamic Sign
#define EXPECTED_SIZE_OTHER 0x300
#define EXPECTED_HASH_OTHER                                                    \
    "0000000000000000000000000000000000000000000000000000000000000000"

typedef struct {
    unsigned size;
    const char *sha256;
} apk_sign_key_t;

#endif /* MANAGER_SIGN_H */
