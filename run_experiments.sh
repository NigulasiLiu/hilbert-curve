#!/bin/bash

# 定义输出目录
OUTPUT_DIR="./src/dataset"
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "输出目录不存在，正在创建：$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"
    if [ $? -ne 0 ]; then
        echo "目录创建失败：$OUTPUT_DIR" >&2
        exit 1
    fi
fi

# 定义实验命令和对应的输出文件
declare -A experiments=(
    ["DSSESearchVariesByOAndL"]="java -XX:+UseG1GC -cp \"target/classes:target/dependency/*\" org.davidmoten.Experiment.PerformanceEval.DSSESearchVariesByOAndL"
    ["Con1SearchVariesByOAndL"]="java -XX:+UseG1GC -cp \"target/classes:target/dependency/*\" org.davidmoten.Experiment.PerformanceEval.Con1SearchVariesByOAndL"
)

# 遍历并运行实验
for experiment in "${!experiments[@]}"; do
    OUTPUT_FILE="$OUTPUT_DIR/$experiment.txt"

    # 检查是否存在目标文件，避免覆盖旧的实验结果
    if [ -e "$OUTPUT_FILE" ]; then
        echo "警告：文件 $OUTPUT_FILE 已存在，备份旧文件"
        mv "$OUTPUT_FILE" "${OUTPUT_FILE}.bak"
        if [ $? -ne 0 ]; then
            echo "备份失败：$OUTPUT_FILE" >&2
            exit 1
        fi
    fi

    echo "正在运行: ${experiments[$experiment]}"
    echo "输出文件: $OUTPUT_FILE"

    # 运行命令并将输出重定向到对应文件
    eval ${experiments[$experiment]} > "$OUTPUT_FILE" 2>&1

    if [ $? -eq 0 ]; then
        echo "实验 $experiment 正常完成"
    else
        echo "实验 $experiment 失败，请检查 $OUTPUT_FILE" >&2
        exit 1
    fi
done

echo "所有实验完成"
