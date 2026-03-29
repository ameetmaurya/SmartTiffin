from flask import Flask, request, jsonify
import random

app = Flask(__name__)

# Mock Model for Demand Prediction
@app.route('/predict_demand', methods=['POST'])
def predict():
    data = request.json
    day = data.get('day_of_week')

    # Simple Logic: Weekends have less demand, rainy days have more
    base_demand = 150

    if day in ['SATURDAY', 'SUNDAY']:
        base_demand = 50

    # In a real app, we would load a .pkl model here
    # model = joblib.load('demand_model.pkl')
    # prediction = model.predict(...)

    return jsonify({"prediction": base_demand, "confidence": "95%"})

if __name__ == '__main__':
    app.run(port=5000)