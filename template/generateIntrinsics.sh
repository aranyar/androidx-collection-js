#!/bin/bash

# Generate collections using C intrinsics for KMP
# Uses IntAsLongArray for metadata and calls _intset*, _intObjectMap*, _scatterSet*, _scatterMap* intrinsics

scriptDir=`dirname ${PWD}/${0}`

echo "Generating IntSet intrinsic version..."
sed -e "s/IntSet/IntSet/g" ${scriptDir}/IntSetIntrinsic.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/IntSet.kt

echo "Generating IntObjectMap intrinsic version..."
sed -e "s/IntObjectMap/IntObjectMap/g" ${scriptDir}/IntObjectMapIntrinsic.kt.template > ${scriptDir}/../src/commonMain/kotlin/androidx/collection/IntObjectMap.kt

echo "Done generating intrinsic collections"
