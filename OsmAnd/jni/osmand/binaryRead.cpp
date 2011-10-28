#ifndef _OSMAND_BINARY_READ
#define _OSMAND_BINARY_READ

#include <math.h>
#include <android/log.h>
#include <stdio.h>
#include <fstream>
#include <map>
#include "google/protobuf/io/zero_copy_stream_impl.h"
#include "google/protobuf/wire_format_lite.h"
#include "google/protobuf/wire_format_lite.cc"
#include "google/protobuf/wire_format.h"

#include "common.h"
#include "proto/osmand_odb.pb.h"

char errorMsg[1024];
#define	INT_MAX		0x7fffffff	/* max value for an int */
using namespace google::protobuf;
using namespace google::protobuf::internal;

struct BinaryMapFile;
std::map<std::string, BinaryMapFile*> openFiles;

inline bool readInt(io::CodedInputStream* input, uint32* sz) {
	uint8 buf[4];
	if(!input->ReadRaw(buf, 4)){
		return false;
	}
	*sz = ((buf[0] << 24) + (buf[1] << 16) + (buf[2] << 8) + (buf[3] << 0));
	return true;
}

bool skipFixed32(io::CodedInputStream* input){
	uint32 sz;
	if(!readInt(input, &sz)) {
		return false;
	}
	return input->Skip(sz);
}

bool skipUnknownFields(io::CodedInputStream* input, int tag) {
	if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_FIXED32_LENGTH_DELIMITED) {
		if (!skipFixed32(input)) {
			return false;
		}
	} else if (!WireFormat::SkipField(input, tag, NULL)) {
		return false;
	}
	return true;
}


struct BinaryMapFile {
	io::FileInputStream* input;
	std::string inputName;

	~BinaryMapFile() {
		input->Close();
		delete input;
	}
};
//display google::protobuf::internal::WireFormatLite::GetTagWireType(tag)
// display google::protobuf::internal::WireFormatLite::GetTagFieldNumber(tag)
bool initMapStructure(io::CodedInputStream* input, BinaryMapFile* file) {
#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
	uint32 tag;
	uint32 version = -1;
	uint32 versionConfirm = -2;
	while ((tag = input->ReadTag()) != 0) {
		switch (WireFormatLite::GetTagFieldNumber(tag)) {
		// required uint32 version = 1;
		case OsmAndStructure::kVersionFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &version)));
			break;
		}
//			case OsmAndStructure::kMapIndexFieldNumber : {
//				// TODO
//				break;
//			}
		case OsmAndStructure::kVersionConfirmFieldNumber: {
			DO_((WireFormatLite::ReadPrimitive<uint32, WireFormatLite::TYPE_UINT32>(input, &versionConfirm)));
			break;
		}
		default: {
			if (WireFormatLite::GetTagWireType(tag) == WireFormatLite::WIRETYPE_END_GROUP) {
				return true;
			}
			if (!skipUnknownFields(input, tag)) {
				return false;
			}

			break;
		}
		}
	}
	if (version != versionConfirm) {
		__android_log_print(ANDROID_LOG_WARN, "net.osmand",
				"Corrupted file. It should be ended as it starts with version");
		return false;
	}
#undef DO_
	return true;
}


void loadJniBinaryRead() {
}


extern "C"
JNIEXPORT jboolean JNICALL Java_net_osmand_plus_render_NativeOsmandLibrary_initBinaryMapFile(JNIEnv* ienv,
		jobject obj, jobject path) {
	// Verify that the version of the library that we linked against is
	setGlobalEnv(ienv);
	const char* utf = ienv->GetStringUTFChars((jstring)path, NULL);
	std::string inputName(utf);
	ienv->ReleaseStringUTFChars((jstring)path, utf);

	__android_log_print(ANDROID_LOG_ERROR, "net.osmand", inputName.c_str());

//	std::string inputName = getString((jstring) path);
	// compatible with the version of the headers we compiled against.
	GOOGLE_PROTOBUF_VERIFY_VERSION;
	std::map<std::string, BinaryMapFile*>::iterator iterator;
	if ((iterator = openFiles.find(inputName)) != openFiles.end()) {
		delete iterator->second;
		openFiles.erase(iterator);
	}

	FILE* file = fopen(inputName.c_str(), "r");
	if (file == NULL) {
		sprintf(errorMsg, "File could not be open to read from C : %s", inputName.c_str());
		__android_log_print(ANDROID_LOG_WARN, "net.osmand", errorMsg);
		return false;
	}

	BinaryMapFile* mapFile = new BinaryMapFile();
	mapFile->input = new io::FileInputStream(fileno(file));
	io::CodedInputStream cis (mapFile->input);
	cis.SetTotalBytesLimit(INT_MAX, INT_MAX >> 2);
	if (!initMapStructure(&cis, mapFile)) {
		sprintf(errorMsg, "File not initialised : %s", inputName.c_str());
		__android_log_print(ANDROID_LOG_WARN, "net.osmand", errorMsg);
		delete mapFile;
		return false;
	}
	mapFile->inputName = inputName;

	openFiles.insert(std::pair<std::string, BinaryMapFile*>(inputName, mapFile));
	return true;
}

#endif
