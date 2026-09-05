import csv
import hashlib
from datetime import datetime
from pathlib import Path


def check_password_strength(value):
	checks = {
		"长度至少 8 位": len(value) >= 8,
		"包含大写字母": any(character.isupper() for character in value),
		"包含小写字母": any(character.islower() for character in value),
		"包含数字": any(character.isdigit() for character in value),
		"包含特殊符号": any(not character.isalnum() for character in value),
	}
	score = sum(checks.values())
	levels = {0: "很弱", 1: "很弱", 2: "较弱", 3: "中等", 4: "较强", 5: "很强"}
	return levels[score], checks


def print_password_strength(value):
	level, checks = check_password_strength(value)
	print("密码强度：", level)
	for name, passed in checks.items():
		mark = "通过" if passed else "未满足"
		print(f"- {name}：{mark}")
	return level, checks


def print_strength_history(history):
	print("\n==========强度历史==========")
	if not history:
		print("暂无历史记录。")
	else:
		for index, record in enumerate(history, start=1):
			print(f"{index}. {record['time']} | 强度：{record['level']}")
			failed_checks = [name for name, passed in record["checks"].items() if not passed]
			if failed_checks:
				print("   未满足：", "、".join(failed_checks))
			else:
				print("   所有强度条件均已满足")
	print("============================")


def export_to_password_manager(value):
	file_name = f"密码管理器导入_{datetime.now():%Y%m%d_%H%M%S}.csv"
	file_path = Path.cwd() / file_name
	with file_path.open("w", newline="", encoding="utf-8-sig") as file:
		writer = csv.writer(file)
		writer.writerow(["name", "username", "password", "url", "notes"])
		writer.writerow(["XGstudio密码", "", value, "", "由哈希转换程序导出"])
	print("密码管理器导入文件已生成：", file_path)
	print("注意：CSV 文件包含明文密码，请导入后立即删除此文件。")


def calculate_hashes(value):
	encoded_value = value.encode("utf-8")
	sha1 = hashlib.sha1(encoded_value).hexdigest()
	sha256 = hashlib.sha256(encoded_value).hexdigest()
	return sha1, sha256


def main():
	history = []

	while True:
		while True:
			value = input("请输入要转换的内容：")
			if value:
				break
			print("输入不能为空，请重新输入。")

		sha1, sha256 = calculate_hashes(value)
		level, checks = print_password_strength(value)
		history.append({
			"time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
			"level": level,
			"checks": checks,
		})
		print("SHA-1：", sha1)
		print("SHA-256：", sha256)

		while True:
			export_choice = input("是否生成密码管理器导入文件？(y/n)：").strip().lower()
			if export_choice == "y":
				export_to_password_manager(value)
				break
			if export_choice == "n":
				break
			print("输入无效，请输入 y 或 n。")

		while True:
			choice = input("是否需要再输入一个？(y/n)：").strip().lower()
			if choice == "y":
				print()
				break
			if choice == "n":
				print_strength_history(history)
				print("程序结束。")
				return
			print("输入无效，请输入 y 或 n。")


if __name__ == "__main__":
	main()
