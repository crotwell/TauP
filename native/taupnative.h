#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jni.h>

typedef struct TauPStruct {
	JNIEnv *env;
	JavaVM *jvm;
	jclass toolClass,
		arrivalClass;
	jobject tool;
	jmethodID depthCorID, 
		parsePhaseID,
		clearPhasesID, 
		calcID,
		getNumArrivalsID,
		getArrivalID,
		getArrivalTime,
		getArrivalDist,
		getArrivalRayParam,
		getArrivalSourceDepth,
		getArrivalName,
		getArrivalPuristName;
	jobjectArray args;
} TauPStruct;

int TauPInit(TauPStruct*, char *);
int TauPSetDepth(TauPStruct, double);
int TauPClearPhases(TauPStruct, double);
int TauPAppendPhases(TauPStruct, char *);
int TauPCalculate(TauPStruct, double);
int TauPGetNumArrivals(TauPStruct);
jobject TauPGetArrival(TauPStruct, int);
double TauPGetArrivalTime(TauPStruct, int);
double TauPGetArrivalDist(TauPStruct, int);
double TauPGetArrivalRayParam(TauPStruct, int);
double TauPGetArrivalSourceDepth(TauPStruct, int);
char* TauPGetArrivalName(TauPStruct, int);
char* TauPGetArrivalPuristName(TauPStruct, int);
int TauPDestroy(TauPStruct);

