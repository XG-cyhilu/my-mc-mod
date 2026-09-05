import hashlib
import secrets
import string


def generate_password(length=8):
	if length < 2:
		raise ValueError("密码长度至少需要为 2 位")

	characters = string.ascii_letters + string.digits
	password_chars = [
		secrets.choice(string.ascii_letters),
		secrets.choice(string.digits),
	]
	password_chars.extend(secrets.choice(characters) for _ in range(length - 2))
	secrets.SystemRandom().shuffle(password_chars)
	return "".join(password_chars)


def calculate_sha1(value):
	return hashlib.sha1(value.encode("utf-8")).hexdigest()


def calculate_sha256(value):
	return hashlib.sha256(value.encode("utf-8")).hexdigest()


def main():
	while True:
		password = generate_password()
		print("生成的8位密码：", password)
		print("SHA-1：", calculate_sha1(password))
		print("SHA-256：", calculate_sha256(password))

		while True:
			choice = input("是否重新生成？(y/n)：").strip().lower()
			if choice == "y":
				print()
				break
			if choice == "n":
				print("程序结束。")
				return
			print("输入无效，请输入 y 或 n。")


if __name__ == "__main__":
	main()
