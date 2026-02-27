#ifndef MANAGER_SIGN_H
#define MANAGER_SIGN_H

// weishu/KernelSU
#define EXPECTED_SIZE_WEISHU 0x033b
#define EXPECTED_HASH_WEISHU                                                   \
    "c371061b19d8c7d7d6133c6a9bafe198fa944e50c1b31c9d8daa8d7f1fc2d2d6"

// 5ec1cff/KernelSU
#define EXPECTED_SIZE_5EC1CFF 384
#define EXPECTED_HASH_5EC1CFF                                                  \
    "7e0c6d7278a3bb8e364e0fcba95afaf3666cf5ff3c245a3b63c8833bd0445cc4"

// rsuntk/KernelSU
#define EXPECTED_SIZE_RSUNTK 0x396
#define EXPECTED_HASH_RSUNTK                                                   \
    "f415f4ed9435427e1fdf7f1fccd4dbc07b3d6b8751e4dbcec6f19671f427870b"

// ShirkNeko/KernelSU
#define EXPECTED_SIZE_SHIRKNEKO 0x35c
#define EXPECTED_HASH_SHIRKNEKO                                                \
    "947ae944f3de4ed4c21a7e4f7953ecf351bfa2b36239da37a34111ad29993eef"

// Neko/KernelSU
#define EXPECTED_SIZE_NEKO 0x29c
#define EXPECTED_HASH_NEKO                                                     \
    "946b0557e450a6430a0ba6b6bccee5bc12953ec8735d55e26139b0ec12303b21"

// MamboSU/MamboSU
#define EXPECTED_SIZE_MAMBO 0x384
#define EXPECTED_HASH_MAMBO                                                 \
    "a9462b8b98ea1ca7901b0cbdcebfaa35f0aa95e51b01d66e6b6d2c81b97746d8"

// KOWX712/KernelSU
#define EXPECTED_SIZE_KOWX712 0x375
#define EXPECTED_HASH_KOWX712                                                 \
    "484fcba6e6c43b1fb09700633bf2fb4758f13cb0b2f4457b80d075084b26c588"
    
// KernelSU-Next/KernelSU-Next
#define EXPECTED_SIZE_KSU_NEXT 0x3e6
#define EXPECTED_HASH_KSU_NEXT                                                 \
    "79e590113c4c4c0c222978e413a5faa801666957b1212a328e46c00c69821bf7"
    
// KernelSU-WILD/KernelSU-WILD
#define EXPECTED_SIZE_WILD 0x381
#define EXPECTED_HASH_WILD                                                     \
    "52d52d8c8bfbe53dc2b6ff1c613184e2c03013e090fe8905d8e3d5dc2658c2e4"

// KernelSU-pershoot/KernelSU-pershoot
#define EXPECTED_SIZE_pershoot 0x338
#define EXPECTED_HASH_pershoot                                                     \
    "f26471a28031130362bce7eebffb9a0b8afc3095f163ce0c75a309f03b644a1f"
    
// VorteXSU/VorteXSU
#define EXPECTED_SIZE_VORTEXSU 0x381
#define EXPECTED_HASH_VORTEXSU                                                    \
    "67eec44718428adad14e6a9dca57822759aba7e77a8cad7071f6f6704df8bb48"

// kingfinik98/kernelSU (VorteXSU)
#define EXPECTED_SIZE_VORTEX 0x385
#define EXPECTED_HASH_VORTEX                                                    \
    "2b8a1087ec94d19ed3c0aa468fb6dcc3e0af5ea0d683bd64da20051847184e8e"

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
